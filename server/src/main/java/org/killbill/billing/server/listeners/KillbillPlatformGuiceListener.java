/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2022 Equinix, Inc
 * Copyright 2014-2022 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.server.listeners;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.killbill.billing.lifecycle.api.BusService;
import org.killbill.billing.lifecycle.api.Lifecycle;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.config.DefaultKillbillConfigSource;
import org.killbill.billing.platform.glue.KillBillPlatformModuleBase;
import org.killbill.billing.server.config.KillbillServerConfig;
import org.killbill.billing.server.healthchecks.KillbillHealthcheck;
import org.killbill.billing.server.healthchecks.KillbillPluginsHealthcheck;
import org.killbill.billing.server.healthchecks.KillbillQueuesHealthcheck;
import org.killbill.billing.server.metrics.InstrumentedAppender;
import org.killbill.billing.server.modules.KillbillPlatformModule;
import org.killbill.bus.api.PersistentBus;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.health.api.HealthCheckRegistry;
import org.killbill.commons.metrics.api.MetricRegistry;
import org.killbill.commons.metrics.modules.StatsModule;
import org.killbill.commons.metrics.servlets.HealthCheckServlet;
import org.killbill.commons.metrics.servlets.InstrumentedFilter;
import org.killbill.commons.metrics.servlets.MetricsServlet;
import org.killbill.commons.skeleton.listeners.GuiceServletContextListener;
import org.killbill.commons.skeleton.modules.BaseServerModuleBuilder;
import org.killbill.commons.skeleton.modules.JMXModule;
import org.killbill.commons.utils.annotation.VisibleForTesting;
import org.killbill.notificationq.api.NotificationQueueService;
import org.skife.config.AugmentedConfigurationObjectFactory;
import org.skife.config.ConfigSource;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weakref.jmx.MBeanExporter;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.StatusViaSLF4JLoggerFactory;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;

public class KillbillPlatformGuiceListener extends GuiceServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(KillbillPlatformGuiceListener.class);

    public static final List<String> METRICS_SERVLETS_PATHS = List.of("/1.0/healthcheck", "/1.0/metrics", "/1.0/threads");

    protected KillbillHealthcheck killbillHealthcheck;
    protected KillbillServerConfig config;
    protected KillbillConfigSource configSource;
    protected Injector injector;
    protected Lifecycle killbillLifecycle;
    protected BusService killbillBusService;
    protected EmbeddedDB mainEmbeddedDB;
    protected EmbeddedDB shiroEmbeddedDB;
    protected EmbeddedDB osgiEmbeddedDB;

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        try {
            initializeConfig();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // Will call super.contextInitialized(event)
        initializeGuice(event);

        initializeMetrics(event);

        registerEhcacheMBeans();

        startLifecycle();

        // The host will be put in rotation in KillbillGuiceFilter, once Jersey is fully initialized
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        putOutOfRotation();

        super.contextDestroyed(sce);

        // Guice error, no need to fill the screen with useless stack traces
        if (killbillLifecycle == null) {
            return;
        }

        stopLifecycle();

        stopEmbeddedDBs();

        removeJMXExports();

        stopLogging();
    }

    protected void initializeConfig() throws IOException, URISyntaxException {
        configSource = getConfigSource();

        final AugmentedConfigurationObjectFactory configFactory = new AugmentedConfigurationObjectFactory(new KillbillPlatformConfigSource(configSource));
        config = configFactory.build(KillbillServerConfig.class);
    }

    protected KillbillConfigSource getConfigSource() throws IOException, URISyntaxException {
        return new DefaultKillbillConfigSource();
    }

    protected void initializeGuice(final ServletContextEvent event) {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        guiceModules = List.of(getServletModule(),
                               new JMXModule(KillbillHealthcheck.class, KillbillQueuesHealthcheck.class, NotificationQueueService.class, PersistentBus.class),
                               new StatsModule(METRICS_SERVLETS_PATHS.get(0),
                                               METRICS_SERVLETS_PATHS.get(1),
                                               METRICS_SERVLETS_PATHS.get(2),
                                               List.of(KillbillHealthcheck.class, KillbillPluginsHealthcheck.class, KillbillQueuesHealthcheck.class)),
                               getModule(event.getServletContext()));

        // Start the Guice machinery
        super.contextInitialized(event);

        injector = injector(event);
        event.getServletContext().setAttribute(Injector.class.getName(), injector);

        // Already started at this point - we just need the instance for shutdown
        mainEmbeddedDB = injector.getInstance(EmbeddedDB.class);
        shiroEmbeddedDB = injector.getInstance(Key.get(EmbeddedDB.class, Names.named(KillBillPlatformModuleBase.SHIRO_DATA_SOURCE_ID)));
        osgiEmbeddedDB = injector.getInstance(Key.get(EmbeddedDB.class, Names.named(KillBillPlatformModuleBase.OSGI_DATA_SOURCE_ID)));

        killbillLifecycle = injector.getInstance(Lifecycle.class);
        killbillBusService = injector.getInstance(BusService.class);

        killbillHealthcheck = injector.getInstance(KillbillHealthcheck.class);
    }

    protected ServletModule getServletModule() {
        final BaseServerModuleBuilder builder = new BaseServerModuleBuilder();
        return builder.build();
    }

    protected Module getModule(final ServletContext servletContext) {
        return new KillbillPlatformModule(servletContext, config, configSource);
    }

    protected void initializeMetrics(final ServletContextEvent event) {
        event.getServletContext().setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, injector.getInstance(HealthCheckRegistry.class));

        final MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
        event.getServletContext().setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);
        event.getServletContext().setAttribute(InstrumentedFilter.REGISTRY_ATTRIBUTE, metricRegistry);

        // Instrument Logback
        final Object factory = LoggerFactory.getILoggerFactory();
        if ("ch.qos.logback.classic.LoggerContext".equals(factory.getClass().getName())) {
            final ch.qos.logback.classic.Logger root = ((ch.qos.logback.classic.LoggerContext) factory).getLogger(Logger.ROOT_LOGGER_NAME);

            final InstrumentedAppender metrics = new InstrumentedAppender(metricRegistry);
            metrics.setContext(root.getLoggerContext());
            metrics.start();
            root.addAppender(metrics);
        }
    }

    protected void registerEhcacheMBeans() {
        // Ehcache 3.x does not have any MBean integration for now.
        // See https://groups.google.com/d/msg/ehcache-users/UFRXilVykyE/ENEFyvpxAgAJ
    }

    protected void startLifecycle() {
        startLifecycleStage1();

        // Fire all Startup levels up to service start
        killbillLifecycle.fireStartupSequencePriorEventRegistration();

        startLifecycleStage2();

        // Let's start!
        killbillLifecycle.fireStartupSequencePostEventRegistration();

        startLifecycleStage3();
    }

    protected void startLifecycleStage1() {
    }

    protected void startLifecycleStage2() {
    }

    protected void startLifecycleStage3() {
    }

    protected void putOutOfRotation() {
        if (killbillHealthcheck != null) {
            killbillHealthcheck.putOutOfRotation();

            if (config.getShutdownDelay() != null && config.getShutdownDelay().getMillis() > 0) {
                logger.info("Delaying shutdown sequence for {}ms", config.getShutdownDelay().getMillis());
                try {
                    Thread.sleep(config.getShutdownDelay().getMillis());
                } catch (final InterruptedException e) {
                    logger.warn("Interrupted while sleeping", e);
                    Thread.currentThread().interrupt();
                }
                logger.info("Resuming shutdown sequence");
            }
        }
    }

    protected void stopLifecycle() {
        stopLifecycleStage1();

        killbillLifecycle.fireShutdownSequencePriorEventUnRegistration();

        stopLifecycleStage2();

        // Complete shutdown sequence
        killbillLifecycle.fireShutdownSequencePostEventUnRegistration();

        stopLifecycleStage3();
    }

    protected void stopLifecycleStage1() {
    }

    protected void stopLifecycleStage2() {
    }

    protected void stopLifecycleStage3() {
    }

    protected void stopEmbeddedDBs() {
        stopEmbeddedDB(osgiEmbeddedDB);
        stopEmbeddedDB(shiroEmbeddedDB);
        stopEmbeddedDB(mainEmbeddedDB);
    }

    protected void stopEmbeddedDB(final EmbeddedDB embeddedDB) {
        if (embeddedDB != null) {
            try {
                embeddedDB.stop();
            } catch (final IOException ignored) {
            }
        }
    }

    private void removeJMXExports() {
        final MBeanExporter mBeanExporter = injector.getInstance(MBeanExporter.class);
        if (mBeanExporter != null) {
            mBeanExporter.unexportAllAndReportMissing();
        }
    }

    protected void stopLogging() {
        // We don't use LogbackServletContextListener to make sure Logback is up until the end
        final ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
        if (iLoggerFactory instanceof LoggerContext) {
            final LoggerContext loggerContext = (LoggerContext) iLoggerFactory;
            final ContextAware contextAwareBase = new ContextAwareBase();
            contextAwareBase.setContext(loggerContext);
            StatusViaSLF4JLoggerFactory.addInfo("About to stop " + loggerContext.getClass().getCanonicalName() + " [" + loggerContext.getName() + "]", this);
            loggerContext.stop();
        }
    }

    @VisibleForTesting
    public Injector getInstantiatedInjector() {
        return injector;
    }

    private static final class KillbillPlatformConfigSource implements ConfigSource {

        private final KillbillConfigSource configSource;

        private KillbillPlatformConfigSource(final KillbillConfigSource configSource) {
            this.configSource = configSource;
        }

        @Override
        public String getString(final String propertyName) {
            return configSource.getString(propertyName);
        }
    }
}
