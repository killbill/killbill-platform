/*
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

package org.killbill.billing.osgi.bundles.prometheus;

import java.util.Hashtable;

import javax.servlet.Servlet;

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.commons.metrics.api.MetricRegistry;
import org.osgi.framework.BundleContext;

public class Activator extends KillbillActivatorBase {

    public static final String BUNDLE_NAME = "killbill-prometheus";

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        // Feed Kill Bill metrics to a custom Prometheus Collector
        final MetricRegistry kbRegistry = this.metricRegistry.getMetricRegistry();
        final KillBillCollector killBillCollector = new KillBillCollector(kbRegistry);
        killBillCollector.register();

        // Register a servlet to expose metrics, to be read by the Prometheus server.
        registerServlet(context, new KillBillMetricsServlet());
    }

    private void registerServlet(final BundleContext context, final Servlet servlet) {
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, BUNDLE_NAME);
        registrar.registerService(context, Servlet.class, servlet, props);
    }
}
