/*
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

package org.killbill.billing.platform.test.glue;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.killbill.billing.osgi.api.OSGIConfigProperties;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.test.PlatformDBTestingHelper;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.jdbi.notification.DatabaseTransactionNotificationApi;
import org.killbill.commons.jdbi.transaction.NotificationTransactionHandler;
import org.killbill.commons.jdbi.transaction.RestartTransactionRunner;
import org.killbill.queue.DefaultQueueLifecycle;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.tweak.TransactionHandler;

import com.google.inject.name.Names;

public class TestPlatformModuleWithEmbeddedDB extends TestPlatformModule {

    private final DatabaseTransactionNotificationApi databaseTransactionNotificationApi;
    private final TransactionHandler transactionHandler;

    public TestPlatformModuleWithEmbeddedDB(final KillbillConfigSource configSource, final boolean withOSGI, @Nullable final OSGIConfigProperties osgiConfigProperties) {
        super(configSource, withOSGI, osgiConfigProperties, null);
        this.databaseTransactionNotificationApi = new DatabaseTransactionNotificationApi();
        final TransactionHandler notificationTransactionHandler = new NotificationTransactionHandler(databaseTransactionNotificationApi);
        this.transactionHandler = new RestartTransactionRunner(notificationTransactionHandler);
    }

    @Override
    protected void configure() {
        super.configure();
        bind(DatabaseTransactionNotificationApi.class).toInstance(databaseTransactionNotificationApi);
        bind(TransactionHandler.class).toInstance(transactionHandler);
    }

    @Override
    protected void configureEmbeddedDB() {
        final PlatformDBTestingHelper platformDBTestingHelper = PlatformDBTestingHelper.get();
        configureEmbeddedDB(platformDBTestingHelper);
    }

    protected void configureEmbeddedDB(final PlatformDBTestingHelper platformDBTestingHelper) {

        //
        // DBI instance is created through the PlatformDBTestingHelper (by calling the DBIProvider directly instead of using injection)
        // Manually set set the transactionHandler which is required for the STICKY_EVENTS bus mode.
        //
        final DBI dbi = (DBI) platformDBTestingHelper.getDBI();
        dbi.setTransactionHandler(transactionHandler);

        // Bind everything through Guice so they be injected throughout our code.
        final EmbeddedDB instance = platformDBTestingHelper.getInstance();
        bind(EmbeddedDB.class).toInstance(instance);


        bind(DataSource.class).toInstance(platformDBTestingHelper.getDataSource());
        bind(IDBI.class).toInstance(dbi);
        bind(IDBI.class).annotatedWith(Names.named(DefaultQueueLifecycle.QUEUE_NAME)).toInstance(dbi);
    }
}