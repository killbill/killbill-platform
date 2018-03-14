/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014-2017 Groupon, Inc
 * Copyright 2014-2017 The Billing Project, LLC
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
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.killbill.billing.lifecycle.api.BusService;
import org.killbill.billing.lifecycle.api.Lifecycle;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.config.DefaultKillbillConfigSource;
import org.killbill.billing.platform.glue.KillBillPlatformModuleBase;
import org.killbill.billing.server.config.KillbillServerConfig;
import org.killbill.billing.server.config.MetricsGraphiteConfig;
import org.killbill.billing.server.config.MetricsInfluxDbConfig;
import org.killbill.billing.server.healthchecks.KillbillHealthcheck;
import org.killbill.billing.server.healthchecks.KillbillQueuesHealthcheck;
import org.killbill.billing.server.modules.KillbillPlatformModule;
import org.killbill.bus.api.PersistentBus;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.skeleton.listeners.GuiceServletContextListener;
import org.killbill.commons.skeleton.modules.BaseServerModuleBuilder;
import org.killbill.commons.skeleton.modules.JMXModule;
import org.killbill.commons.skeleton.modules.JaxrsJacksonModule;
import org.killbill.commons.skeleton.modules.StatsModule;
import org.killbill.notificationq.api.NotificationQueueService;
import org.skife.config.ConfigSource;
import org.skife.config.ConfigurationObjectFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weakref.jmx.MBeanExporter;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.StatusViaSLF4JLoggerFactory;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import com.izettle.metrics.dw.InfluxDbReporterFactory;

public class KillbillPlatformGuiceListener extends GuiceServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(KillbillPlatformGuiceListener.class);

    public static final ImmutableList<String> METRICS_SERVLETS_PATHS = ImmutableList.<String>of("/1.0/healthcheck", "/1.0/metrics", "/1.0/ping", "/1.0/threads");

    protected KillbillHealthcheck killbillHealthcheck;
    protected KillbillServerConfig config;
    protected KillbillConfigSource configSource;
    protected MetricsGraphiteConfig metricsGraphiteConfig;
    protected MetricsInfluxDbConfig metricsInfluxDbConfig;
    protected Injector injector;
    protected Lifecycle killbillLifecycle;
    protected BusService killbillBusService;
    protected EmbeddedDB mainEmbeddedDB;
    protected EmbeddedDB shiroEmbeddedDB;
    protected EmbeddedDB osgiEmbeddedDB;
    protected JmxReporter metricsJMXReporter;

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

        stopMetrics();

        removeJMXExports();

        stopLogging();
    }

    protected void initializeConfig() throws IOException, URISyntaxException {
        configSource = getConfigSource();

        final ConfigurationObjectFactory configFactory = new ConfigurationObjectFactory(new KillbillPlatformConfigSource(configSource));
        config = configFactory.build(KillbillServerConfig.class);
        metricsGraphiteConfig = configFactory.build(MetricsGraphiteConfig.class);
        metricsInfluxDbConfig = configFactory.build(MetricsInfluxDbConfig.class);
    }

    protected KillbillConfigSource getConfigSource() throws IOException, URISyntaxException {
        return new DefaultKillbillConfigSource();
    }

    protected void initializeGuice(final ServletContextEvent event) {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        guiceModules = ImmutableList.<Module>of(getServletModule(),
                                                getJacksonModule(),
                                                new JMXModule(KillbillHealthcheck.class, KillbillQueuesHealthcheck.class, NotificationQueueService.class, PersistentBus.class),
                                                new StatsModule(METRICS_SERVLETS_PATHS.get(0),
                                                                METRICS_SERVLETS_PATHS.get(1),
                                                                METRICS_SERVLETS_PATHS.get(2),
                                                                METRICS_SERVLETS_PATHS.get(3),
                                                                ImmutableList.<Class<? extends HealthCheck>>of(KillbillHealthcheck.class, KillbillQueuesHealthcheck.class, ThreadDeadlockHealthCheck.class)),
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

    protected JaxrsJacksonModule getJacksonModule() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new JaxrsJacksonModule(objectMapper);
    }

    protected void initializeMetrics(final ServletContextEvent event) {
        final MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        final MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);

        // Instrument SLF4J
        final Object factory = LoggerFactory.getILoggerFactory();
        if ("ch.qos.logback.classic.LoggerContext".equals(factory.getClass().getName())) {
            final ch.qos.logback.classic.Logger root = ((ch.qos.logback.classic.LoggerContext) factory).getLogger(Logger.ROOT_LOGGER_NAME);

            final com.codahale.metrics.logback.InstrumentedAppender metrics = new com.codahale.metrics.logback.InstrumentedAppender(metricRegistry);
            metrics.setContext(root.getLoggerContext());
            metrics.start();
            root.addAppender(metrics);
        } else {
            logger.info("{} not a logback logger factory, {} not started", factory, metricRegistry);
        }

        // Instrument the JVM
        registerAll("buffers", new BufferPoolMetricSet(platformMBeanServer), metricRegistry);
        registerAll("classloading", new ClassLoadingGaugeSet(), metricRegistry);
        registerAll("gc", new GarbageCollectorMetricSet(), metricRegistry);
        registerAll("memory", new MemoryUsageGaugeSet(), metricRegistry);
        registerAll("threads", new ThreadStatesGaugeSet(), metricRegistry);

        // Expose metrics via JMX
        metricsJMXReporter = JmxReporter.forRegistry(metricRegistry).registerWith(platformMBeanServer).build();
        metricsJMXReporter.start();

        // stream metric values to a Graphite server
        if (metricsGraphiteConfig.isGraphiteReportingEnabled()) {
            final Graphite graphite = new Graphite(new InetSocketAddress(metricsGraphiteConfig.getHostname(),
                                                                         metricsGraphiteConfig.getPort()));
            final GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry)
                                                              .prefixedWith(metricsGraphiteConfig.getPrefix())
                                                              .convertRatesTo(TimeUnit.SECONDS)
                                                              .convertDurationsTo(TimeUnit.NANOSECONDS)
                                                              .filter(MetricFilter.ALL)
                                                              .build(graphite);

            reporter.start(metricsGraphiteConfig.getInterval(), TimeUnit.SECONDS);

            logger.info(String.format("reporting metrics to %s:%d",
                                      metricsGraphiteConfig.getHostname(), metricsGraphiteConfig.getPort()));
        }

        // stream metric values to a InfluxDB server
        if (metricsInfluxDbConfig.isInfluxDbReportingEnabled()) {
            final InfluxDbReporterFactory influxDbReporterFactory = new InfluxDbReporterFactory();
            influxDbReporterFactory.setRateUnit(TimeUnit.SECONDS);
            influxDbReporterFactory.setDurationUnit(TimeUnit.NANOSECONDS);
            influxDbReporterFactory.setHost(metricsInfluxDbConfig.getHostname());
            influxDbReporterFactory.setPort(metricsInfluxDbConfig.getPort());
            influxDbReporterFactory.setReadTimeout(metricsInfluxDbConfig.getSocketTimeout());
            influxDbReporterFactory.setDatabase(metricsInfluxDbConfig.getDatabase());
            influxDbReporterFactory.setPrefix(metricsInfluxDbConfig.getPrefix());
            influxDbReporterFactory.setSenderType(metricsInfluxDbConfig.getSenderType());

            final ScheduledReporter reporter = influxDbReporterFactory.build(metricRegistry);

            reporter.start(metricsInfluxDbConfig.getInterval(), TimeUnit.SECONDS);

            logger.info(String.format("reporting metrics to %s:%d",
                                      metricsInfluxDbConfig.getHostname(), metricsInfluxDbConfig.getPort()));
        }

        event.getServletContext().setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, injector.getInstance(HealthCheckRegistry.class));
        event.getServletContext().setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);
        event.getServletContext().setAttribute(InstrumentedFilter.REGISTRY_ATTRIBUTE, metricRegistry);
    }

    private void registerAll(final String prefix, final MetricSet metricSet, final MetricRegistry registry) {
        for (final Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            if (entry.getValue() instanceof MetricSet) {
                registerAll(prefix + "." + entry.getKey(), (MetricSet) entry.getValue(), registry);
            } else {
                registry.register(prefix + "." + entry.getKey(), entry.getValue());
            }
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

    protected void stopMetrics() {
        if (metricsJMXReporter != null) {
            metricsJMXReporter.stop();
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
