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

package org.killbill.billing.osgi.libs.killbill;

import org.killbill.commons.metrics.api.MetricRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class OSGIMetricRegistry extends OSGIKillbillLibraryBase {

    private static final String METRICS_REGISTRY_SERVICE_NAME = "org.killbill.billing.osgi.api.metrics.MetricRegistry";

    private final ServiceTracker<MetricRegistry, MetricRegistry> metricRegistryServiceTracker;

    public OSGIMetricRegistry(final BundleContext context) {
        metricRegistryServiceTracker = new ServiceTracker<MetricRegistry, MetricRegistry>(context, METRICS_REGISTRY_SERVICE_NAME, null);
        metricRegistryServiceTracker.open();
    }

    public void close() {
        if (metricRegistryServiceTracker != null) {
            metricRegistryServiceTracker.close();
        }
    }

    public MetricRegistry getMetricRegistry() {
        return withServiceTracker(metricRegistryServiceTracker, new APICallback<MetricRegistry, MetricRegistry>(METRICS_REGISTRY_SERVICE_NAME) {
            @Override
            public MetricRegistry executeWithService(final MetricRegistry service) {
                return metricRegistryServiceTracker.getService();
            }
        });
    }
}
