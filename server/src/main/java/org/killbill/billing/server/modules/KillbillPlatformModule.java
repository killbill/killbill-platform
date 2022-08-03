/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

package org.killbill.billing.server.modules;

import javax.inject.Provider;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.killbill.billing.lifecycle.glue.BusModule;
import org.killbill.billing.lifecycle.glue.LifecycleModule;
import org.killbill.billing.osgi.MetricRegistryServiceRegistration;
import org.killbill.billing.osgi.ServiceRegistryServiceRegistration;
import org.killbill.billing.osgi.api.OSGIServiceRegistration;
import org.killbill.billing.osgi.api.OSGISingleServiceRegistration;
import org.killbill.billing.osgi.api.ServiceDiscoveryRegistry;
import org.killbill.billing.osgi.glue.DefaultOSGIModule;
import org.killbill.billing.osgi.glue.OSGIDataSourceConfig;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.config.DefaultKillbillConfigSource;
import org.killbill.billing.platform.glue.KillBillPlatformModuleBase;
import org.killbill.billing.platform.glue.NotificationQueueModule;
import org.killbill.billing.platform.glue.ReferenceableDataSourceSpyProvider;
import org.killbill.billing.platform.jndi.JNDIManager;
import org.killbill.billing.server.config.KillbillServerConfig;
import org.killbill.billing.server.metrics.KillbillPluginsMetricRegistry;
import org.killbill.clock.Clock;
import org.killbill.clock.ClockMock;
import org.killbill.clock.DefaultClock;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.jdbi.guice.DBIProvider;
import org.killbill.commons.jdbi.guice.DaoConfig;
import org.killbill.commons.jdbi.metrics.KillBillTimingCollector;
import org.killbill.commons.jdbi.notification.DatabaseTransactionNotificationApi;
import org.killbill.commons.jdbi.transaction.NotificationTransactionHandler;
import org.killbill.commons.jdbi.transaction.RestartTransactionRunner;
import org.killbill.commons.metrics.api.MetricRegistry;
import org.killbill.commons.metrics.guice.MetricsInstrumentationModule;
import org.killbill.queue.DefaultQueueLifecycle;
import org.skife.config.ConfigSource;
import org.skife.config.ConfigurationObjectFactory;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.TimingCollector;
import org.skife.jdbi.v2.tweak.TransactionHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class KillbillPlatformModule extends KillBillPlatformModuleBase {

    protected final ServletContext servletContext;

    protected final KillbillServerConfig serverConfig;

    protected DaoConfig daoConfig;
    protected MainRoDaoConfig mainRoDataSourceConfig;
    protected EmbeddedDB mainEmbeddedDB;
    protected EmbeddedDB mainRoEmbeddedDB;
    protected EmbeddedDB shiroEmbeddedDB;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public KillbillPlatformModule(final ServletContext servletContext, final KillbillServerConfig serverConfig, final KillbillConfigSource configSource) {
        super(configSource);
        this.servletContext = servletContext;
        this.serverConfig = serverConfig;
    }

    @Override
    protected void configure() {
        configureJackson();
        configureClock();
        configureDao();
        configureConfig();
        configureEmbeddedDBs();
        configureLifecycle();
        configureBuses();
        configureNotificationQ();
        configureOSGI();
        configureJNDI();
        configureMetrics();
    }

    protected void configureJackson() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        bind(ObjectMapper.class).toInstance(objectMapper);
    }

    protected void configureClock() {
        if (serverConfig.isTestModeEnabled()) {
            if (serverConfig.isRedisClockEnabled()) {
                bind(Clock.class).toProvider(DistributedClockProvider.class).asEagerSingleton();
            } else {
                bind(Clock.class).to(ClockMock.class).asEagerSingleton();
            }
        } else {
            bind(Clock.class).to(DefaultClock.class).asEagerSingleton();
        }
    }

    protected void configureDao() {
        daoConfig = new ConfigurationObjectFactory(skifeConfigSource).build(DaoConfig.class);
        bind(DaoConfig.class).toInstance(daoConfig);

        mainRoDataSourceConfig = new ConfigurationObjectFactory(skifeConfigSource).build(MainRoDaoConfig.class);
        bind(MainRoDaoConfig.class).toInstance(mainRoDataSourceConfig);

        final DatabaseTransactionNotificationApi databaseTransactionNotificationApi = new DatabaseTransactionNotificationApi();
        bind(DatabaseTransactionNotificationApi.class).toInstance(databaseTransactionNotificationApi);

        final TransactionHandler notificationTransactionHandler = new NotificationTransactionHandler(databaseTransactionNotificationApi);
        final TransactionHandler ourSuperTunedTransactionHandler = new RestartTransactionRunner(notificationTransactionHandler);
        bind(TransactionHandler.class).toInstance(ourSuperTunedTransactionHandler);

        bind(IDBI.class).toProvider(DBIProvider.class).asEagerSingleton();
        bind(IDBI.class).annotatedWith(Names.named(DefaultQueueLifecycle.QUEUE_NAME)).toProvider(DBIProvider.class).asEagerSingleton();
        bind(IDBI.class).annotatedWith(Names.named(MAIN_RO_DATA_SOURCE_ID)).toProvider(RODBIProvider.class).asEagerSingleton();
    }

    // https://code.google.com/p/google-guice/issues/detail?id=627
    // https://github.com/google/guice/issues/627
    // https://github.com/google/guice/commit/6b7e7187bd074d3f2df9b04e17fa01e7592f295c
    @Provides
    @Singleton
    protected DataSource provideDataSourceInAComplicatedWayBecauseOf627(final Injector injector) {
        return provideDataSourceInAComplicatedWayBecauseOf627(injector, daoConfig, mainEmbeddedDB, MAIN_DATA_SOURCE_ID);
    }

    @Provides
    @Named(MAIN_RO_DATA_SOURCE_ID)
    @Singleton
    protected DataSource provideMainRoDataSourceInAComplicatedWayBecauseOf627(final Injector injector) {
        if (mainRoDataSourceConfig.isEnabled()) {
            return provideDataSourceInAComplicatedWayBecauseOf627(injector, mainRoDataSourceConfig, mainRoEmbeddedDB, MAIN_RO_DATA_SOURCE_ID);
        } else {
            // See provideDataSourceInAComplicatedWayBecauseOf627 above
            return injector.getInstance(DataSource.class);
        }
    }

    @Provides
    @Named(SHIRO_DATA_SOURCE_ID)
    @Singleton
    protected DataSource provideShiroDataSourceInAComplicatedWayBecauseOf627(final Injector injector) {
        return provideDataSourceInAComplicatedWayBecauseOf627(injector, daoConfig, shiroEmbeddedDB, SHIRO_DATA_SOURCE_ID);
    }

    protected DataSource provideDataSourceInAComplicatedWayBecauseOf627(final Injector injector, final DaoConfig daoConfig, final EmbeddedDB embeddedDB, final String dataSourceId) {
        final Provider<DataSource> dataSourceSpyProvider = new ReferenceableDataSourceSpyProvider(daoConfig, embeddedDB, dataSourceId);
        injector.injectMembers(dataSourceSpyProvider);
        return dataSourceSpyProvider.get();
    }

    @Provides
    @Singleton
    protected TimingCollector provideTimingCollector(final MetricRegistry metricRegistry) {
        // Metrics / jDBI integration
        return new KillBillTimingCollector(metricRegistry);
    }

    protected void configureConfig() {
        bind(ConfigSource.class).toInstance(skifeConfigSource);
        bind(KillbillServerConfig.class).toInstance(serverConfig);
    }

    protected void configureEmbeddedDBs() {
        mainEmbeddedDB = new EmbeddedDBProvider(daoConfig).get();
        bind(EmbeddedDB.class).toInstance(mainEmbeddedDB);

        // Same database, but different pool: clone the object so the shutdown sequence cleans the pool properly
        shiroEmbeddedDB = new EmbeddedDBProvider(daoConfig).get();
        bind(EmbeddedDB.class).annotatedWith(Names.named(SHIRO_DATA_SOURCE_ID)).toInstance(shiroEmbeddedDB);

        if (mainRoDataSourceConfig.isEnabled()) {
            mainRoEmbeddedDB = new EmbeddedDBProvider(mainRoDataSourceConfig).get();
        } else {
            mainRoEmbeddedDB = mainEmbeddedDB;
        }
        bind(EmbeddedDB.class).annotatedWith(Names.named(MAIN_RO_DATA_SOURCE_ID)).toInstance(mainRoEmbeddedDB);
    }

    protected void configureLifecycle() {
        install(new LifecycleModule());
    }

    protected void configureBuses() {
        install(new BusModule(BusModule.BusType.PERSISTENT, false, configSource));
        install(new BusModule(BusModule.BusType.PERSISTENT, true, configSource));
    }

    protected void configureNotificationQ() {
        install(new NotificationQueueModule(configSource));
    }

    protected void configureOSGI() {
        final OSGIDataSourceConfig osgiDataSourceConfig = new ConfigurationObjectFactory(skifeConfigSource).build(OSGIDataSourceConfig.class);
        final EmbeddedDB osgiEmbeddedDB = new EmbeddedDBProvider(osgiDataSourceConfig).get();
        bind(EmbeddedDB.class).annotatedWith(Names.named(OSGI_DATA_SOURCE_ID)).toInstance(osgiEmbeddedDB);
        install(new DefaultOSGIModule(configSource, (DefaultKillbillConfigSource) configSource, osgiDataSourceConfig, osgiEmbeddedDB));
    }

    protected void configureJNDI() {
        bind(JNDIManager.class).asEagerSingleton();
    }

    protected void configureMetrics() {
        bind(new TypeLiteral<OSGIServiceRegistration<ServiceDiscoveryRegistry>>() {
        }).to(ServiceRegistryServiceRegistration.class).asEagerSingleton();

        final MetricRegistryServiceRegistration metricRegistryServiceRegistration = new MetricRegistryServiceRegistration();
        bind(new TypeLiteral<OSGISingleServiceRegistration<MetricRegistry>>() {
        }).toInstance(metricRegistryServiceRegistration);

        final MetricRegistry metricRegistry = new KillbillPluginsMetricRegistry(metricRegistryServiceRegistration);
        bind(MetricRegistry.class).toInstance(metricRegistry);
        install(MetricsInstrumentationModule.builder().withMetricRegistry(metricRegistry).build());
    }
}
