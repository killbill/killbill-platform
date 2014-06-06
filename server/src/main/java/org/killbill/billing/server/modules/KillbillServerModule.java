/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
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

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.killbill.billing.lifecycle.DefaultLifecycle;
import org.killbill.billing.lifecycle.api.Lifecycle;
import org.killbill.billing.lifecycle.bus.ExternalPersistentBusConfig;
import org.killbill.billing.lifecycle.glue.BusModule;
import org.killbill.billing.lifecycle.glue.PersistentBusProvider;
import org.killbill.billing.osgi.api.ExternalBus;
import org.killbill.billing.osgi.glue.DefaultOSGIModule;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.config.DefaultKillbillConfigSource;
import org.killbill.billing.platform.glue.KillBillModule;
import org.killbill.billing.platform.glue.NotificationQueueModule;
import org.killbill.billing.server.config.KillbillServerConfig;
import org.killbill.bus.api.PersistentBus;
import org.killbill.bus.api.PersistentBusConfig;
import org.killbill.clock.Clock;
import org.killbill.clock.ClockMock;
import org.killbill.clock.DefaultClock;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.jdbi.guice.DBIProvider;
import org.killbill.commons.jdbi.guice.DaoConfig;
import org.killbill.commons.jdbi.guice.DataSourceProvider;
import org.skife.config.ConfigSource;
import org.skife.config.ConfigurationObjectFactory;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

public class KillbillServerModule extends KillBillModule {

    protected final ServletContext servletContext;

    private final KillbillServerConfig serverConfig;

    protected DaoConfig daoConfig;
    protected DBI dbi;

    public KillbillServerModule(final ServletContext servletContext, final KillbillServerConfig serverConfig, final KillbillConfigSource configSource) {
        super(configSource);
        this.servletContext = servletContext;
        this.serverConfig = serverConfig;
    }

    @Override
    protected void configure() {
        configureClock();
        configureDao();
        configureConfig();
        configureEmbeddedDB();
        configureLifecycle();
        configureBus();
        configureNotificationQ();
        configureOSGI();

        installKillbillModules();
    }

    protected void configureClock() {
        if (serverConfig.isTestModeEnabled()) {
            bind(Clock.class).to(ClockMock.class).asEagerSingleton();
        } else {
            bind(Clock.class).to(DefaultClock.class).asEagerSingleton();
        }
    }

    protected void configureDao() {
        // Load mysql driver if needed
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (final Exception ignore) {
        }

        daoConfig = new ConfigurationObjectFactory(skifeConfigSource).build(DaoConfig.class);
        //bind(DaoConfig.class).toInstance(daoConfig);

        final DataSource dataSource = new DataSourceProvider(daoConfig).get();
        bind(DataSource.class).toInstance(dataSource);

        final DBIProvider dbiProvider = new DBIProvider(dataSource);
//        final BasicSqlNameStrategy basicSqlNameStrategy = new BasicSqlNameStrategy();
//        // TODO
//        final TimingCollector timingCollector = new InstrumentedTimingCollector(null, basicSqlNameStrategy);
//        dbiProvider.setTimingCollector(timingCollector);

        dbi = (DBI) dbiProvider.get();
        bind(DBI.class).toInstance(dbi);
        bind(IDBI.class).to(DBI.class).asEagerSingleton();
    }

    protected void configureConfig() {
        bind(ConfigSource.class).toInstance(skifeConfigSource);
        bind(KillbillServerConfig.class).toInstance(serverConfig);
    }

    protected void configureEmbeddedDB() {
        // TODO Pierre Refactor GlobalLockerModule for this to be a real provider?
        final EmbeddedDBProvider embeddedDBProvider = new EmbeddedDBProvider(daoConfig);
        final EmbeddedDB embeddedDB = embeddedDBProvider.get();
        bind(EmbeddedDB.class).toInstance(embeddedDB);
    }

    protected void configureLifecycle() {
        bind(Lifecycle.class).to(DefaultLifecycle.class).asEagerSingleton();
    }

    protected void configureBus() {
        install(new BusModule(BusModule.BusType.PERSISTENT, configSource));
        //install(new MetricsModule(configSource));
    }

    protected void configureNotificationQ() {
        install(new NotificationQueueModule(configSource));
    }

    protected void configureOSGI() {
        final PersistentBusConfig extBusConfig = new ExternalPersistentBusConfig(skifeConfigSource);
        install(new DefaultOSGIModule(configSource, (DefaultKillbillConfigSource) configSource));
        bind(PersistentBusProvider.class).annotatedWith(Names.named(ExternalBus.EXTERNAL_BUS)).toInstance(new PersistentBusProvider(extBusConfig));
        bind(PersistentBus.class).annotatedWith(Names.named(ExternalBus.EXTERNAL_BUS)).toProvider(Key.get(PersistentBusProvider.class, Names.named(ExternalBus.EXTERNAL_BUS))).asEagerSingleton();
    }

    protected void installKillbillModules() {
    }
}
