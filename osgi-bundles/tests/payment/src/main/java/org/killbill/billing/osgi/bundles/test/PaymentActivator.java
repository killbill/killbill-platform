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

package org.killbill.billing.osgi.bundles.test;

import java.util.Dictionary;
import java.util.Hashtable;

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * Test class used by Payment tests-- to test fake OSGI payment bundle
 */
public class PaymentActivator extends KillbillActivatorBase {

    private static final String TEST_PLUGIN_NAME = "osgi-payment-plugin";

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final String bundleName = context.getBundle().getSymbolicName();
        logService.log(LogService.LOG_INFO, "PaymentActivator: starting bundle = " + bundleName);

        registerPaymentApi(context);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        logService.log(LogService.LOG_INFO, "PaymentActivator: stopping bundle");

        super.stop(context);
    }

    private void registerPaymentApi(final BundleContext context) {
        final Dictionary<String, String> props = new Hashtable<String, String>();
        // Same name the beatrix tests expect when using that payment plugin
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, TEST_PLUGIN_NAME);
        registrar.registerService(context, PaymentPluginApi.class, new TestPaymentPluginApi(), props);
    }
}
