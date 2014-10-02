/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
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

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.killbill.billing.lifecycle.glue.BusModule;
import org.killbill.billing.lifecycle.glue.LifecycleModule;
import org.killbill.billing.osgi.glue.DefaultOSGIModule;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.config.DefaultKillbillConfigSource;
import org.killbill.billing.platform.glue.KillBillModule;
import org.killbill.billing.platform.glue.NotificationQueueModule;
import org.killbill.billing.platform.jndi.JNDIManager;
import org.killbill.billing.platform.jndi.ReferenceableDataSourceSpy;
import org.killbill.billing.server.config.KillbillServerConfig;
import org.killbill.clock.Clock;
import org.killbill.clock.ClockMock;
import org.killbill.clock.DefaultClock;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.jdbi.guice.DBIProvider;
import org.killbill.commons.jdbi.guice.DaoConfig;
import org.killbill.commons.jdbi.guice.DataSourceProvider;
import org.killbill.commons.jdbi.notification.DatabaseTransactionNotificationApi;
import org.killbill.commons.jdbi.transaction.NotificationTransactionHandler;
import org.killbill.commons.jdbi.transaction.RestartTransactionRunner;
import org.killbill.queue.DefaultQueueLifecycle;
import org.skife.config.ConfigSource;
import org.skife.config.ConfigurationObjectFactory;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.tweak.TransactionHandler;

import com.google.inject.name.Names;

public class KillbillPlatformModule extends KillBillModule {

    private static final String MAIN_DATA_SOURCE_ID = "main";

    protected final ServletContext servletContext;

    protected final KillbillServerConfig serverConfig;

    protected DaoConfig daoConfig;
    protected DBI dbi;
    protected DBI queueDbi;
    protected EmbeddedDB embeddedDB;

    public KillbillPlatformModule(final ServletContext servletContext, final KillbillServerConfig serverConfig, final KillbillConfigSource configSource) {
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
        configureBuses();
        configureNotificationQ();
        configureOSGI();
        configureJNDI();
    }

    protected void configureClock() {
        if (serverConfig.isTestModeEnabled()) {
            bind(Clock.class).to(ClockMock.class).asEagerSingleton();
        } else {
            bind(Clock.class).to(DefaultClock.class).asEagerSingleton();
        }
    }

    protected void configureDao() {
        daoConfig = new ConfigurationObjectFactory(skifeConfigSource).build(DaoConfig.class);
        bind(DaoConfig.class).toInstance(daoConfig);

        final DataSource realDataSource = new DataSourceProvider(daoConfig, MAIN_DATA_SOURCE_ID).get();
        final DataSource dataSource = new ReferenceableDataSourceSpy(realDataSource, MAIN_DATA_SOURCE_ID);
        bind(DataSource.class).toInstance(dataSource);

        final DatabaseTransactionNotificationApi databaseTransactionNotificationApi = new DatabaseTransactionNotificationApi();
        bind(DatabaseTransactionNotificationApi.class).toInstance(databaseTransactionNotificationApi);

        final TransactionHandler notificationTransactionHandler = new NotificationTransactionHandler(databaseTransactionNotificationApi);
        final RestartTransactionRunner ourSuperTunedTransactionHandler = new RestartTransactionRunner(notificationTransactionHandler);

        final DBIProvider dbiProvider = new DBIProvider(daoConfig, dataSource, ourSuperTunedTransactionHandler);
        dbi = (DBI) dbiProvider.get();
        bind(DBI.class).toInstance(dbi);
        bind(IDBI.class).to(DBI.class).asEagerSingleton();

        final DBIProvider queueDbiProvider = new DBIProvider(null, dataSource, ourSuperTunedTransactionHandler);
        queueDbi = (DBI) queueDbiProvider.get();
        bind(DBI.class).annotatedWith(Names.named(DefaultQueueLifecycle.QUEUE_NAME)).toInstance(queueDbi);
        bind(IDBI.class).annotatedWith(Names.named(DefaultQueueLifecycle.QUEUE_NAME)).toInstance(queueDbi);
    }

    protected void configureConfig() {
        bind(ConfigSource.class).toInstance(skifeConfigSource);
        bind(KillbillServerConfig.class).toInstance(serverConfig);
    }

    protected void configureEmbeddedDB() {
        // TODO Pierre Refactor GlobalLockerModule for this to be a real provider?
        final EmbeddedDBProvider embeddedDBProvider = new EmbeddedDBProvider(daoConfig);
        embeddedDB = embeddedDBProvider.get();
        bind(EmbeddedDB.class).toInstance(embeddedDB);
    }

    protected void configureLifecycle() {
        install(new LifecycleModule());
    }

    protected void configureBuses() {
        install(new BusModule(BusModule.BusType.PERSISTENT, false, configSource));
        install(new BusModule(BusModule.BusType.PERSISTENT, true, configSource));
        //install(new MetricsModule(configSource));
    }

    protected void configureNotificationQ() {
        install(new NotificationQueueModule(configSource));
    }

    protected void configureOSGI() {
        install(new DefaultOSGIModule(configSource, (DefaultKillbillConfigSource) configSource));
    }

    protected void configureJNDI() {
        bind(JNDIManager.class).asEagerSingleton();
    }
}
