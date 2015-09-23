/*
 * Copyright 2015 Groupon, Inc
 *
 * Groupon licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.osgi;

import com.codahale.metrics.MetricRegistry;
import org.killbill.billing.osgi.api.OSGIMetrics;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import javax.inject.Inject;

public class OSGIMetricsFactory implements ServiceFactory<OSGIMetrics> {

    private final MetricRegistry metricRegistry;

    @Inject
    public OSGIMetricsFactory(final MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public OSGIMetrics getService(final Bundle bundle, final ServiceRegistration<OSGIMetrics> registration) {
        return new DefaultOSGIMetrics(metricRegistry, bundle.getSymbolicName());
    }

    @Override
    public void ungetService(final Bundle bundle, final ServiceRegistration<OSGIMetrics> registration, final OSGIMetrics service) {

    }
}
