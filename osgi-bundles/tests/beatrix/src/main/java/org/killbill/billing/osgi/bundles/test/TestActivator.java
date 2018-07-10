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

package org.killbill.billing.osgi.bundles.test;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.notification.plugin.api.NotificationPluginApiRetryException;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.bundles.test.dao.TestDao;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;

/**
 * Test class used by Beatrix OSGI test to verify that:
 * - test bundle is started
 * - test bundle is able to make API call
 * - test bundle is able to register a fake PaymentApi service
 * - test bundle can use the DataSource from Killbill and write on disk
 */
public class TestActivator extends KillbillActivatorBase implements OSGIKillbillEventHandler {

    private static final String TEST_PLUGIN_NAME = "test";

    private final Map<UUID, AtomicInteger> countPerToken = new HashMap<UUID, AtomicInteger>();

    private TestDao testDao;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final String bundleName = context.getBundle().getSymbolicName();
        logService.log(LogService.LOG_INFO, "TestActivator: starting bundle = " + bundleName);

        final IDBI dbi = new DBI(dataSource.getDataSource());
        testDao = new TestDao(dbi);
        testDao.createTable();
        testDao.insertStarted();
        registerPaymentApi(context, testDao);
        dispatcher.registerEventHandlers(this);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        logService.log(LogService.LOG_INFO, "TestActivator: stopping bundle");

        super.stop(context);
    }

    private void registerPaymentApi(final BundleContext context, final TestDao dao) {
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, TEST_PLUGIN_NAME);
        registrar.registerService(context, PaymentPluginApi.class, new TestPaymentPluginApi(dao), props);
    }

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {
        logService.log(LogService.LOG_INFO, "Received external event " + killbillEvent.toString());

        // Specific event for retries
        if (killbillEvent.getEventType() == ExtBusEventType.BLOCKING_STATE) {
            if (countPerToken.get(killbillEvent.getUserToken()) == null) {
                countPerToken.put(killbillEvent.getUserToken(), new AtomicInteger());
            }
            final Integer seen = countPerToken.get(killbillEvent.getUserToken()).incrementAndGet();
            if (!seen.toString().equalsIgnoreCase(killbillEvent.getMetaData())) {
                testDao.insertExternalKey("error-" + seen);
                throw new NotificationPluginApiRetryException();
            } else {
                testDao.insertExternalKey(killbillEvent.getAccountId().toString());
                return;
            }
        }

        // Only looking at account creation
        if (killbillEvent.getEventType() != ExtBusEventType.ACCOUNT_CREATION) {
            return;
        }

        testDao.insertExternalKey(killbillEvent.getAccountId().toString());
    }
}
