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

package org.killbill.killbill.osgi.libs.killbill;

import org.killbill.billing.osgi.api.OSGIMetrics;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class OSGIKillbillMetrics extends OSGIKillbillLibraryBase implements OSGIMetrics {

    private final ServiceTracker killbillTracker;

    public OSGIKillbillMetrics(final BundleContext context) {
        killbillTracker = new ServiceTracker(context, OSGIMetrics.class.getName(), null);
        killbillTracker.open();
    }

    @Override
    public void close() {
        if (killbillTracker != null) {
            killbillTracker.close();
        }
    }

    @Override
    public void markMeter(final String meterName) {
        withServiceTracker(killbillTracker, new APICallback<Void, OSGIMetrics>(OSGIMetrics.class.getName()) {
            @Override
            public Void executeWithService(final OSGIMetrics service) {
                service.markMeter(meterName);
                return null;
            }
        });
    }

    @Override
    public void recordHistogramValue(final String histogramName, final long value) {
        withServiceTracker(killbillTracker, new APICallback<Void, OSGIMetrics>(OSGIMetrics.class.getName()) {
            @Override
            public Void executeWithService(final OSGIMetrics service) {
                service.recordHistogramValue(histogramName, value);
                return null;
            }
        });
    }

    @Override
    public void incrementCounter(final String counterName) {
        withServiceTracker(killbillTracker, new APICallback<Void, OSGIMetrics>(OSGIMetrics.class.getName()) {
            @Override
            public Void executeWithService(final OSGIMetrics service) {
                service.incrementCounter(counterName);
                return null;
            }
        });
    }

    @Override
    public void incrementCounter(final String counterName, final long step) {
        withServiceTracker(killbillTracker, new APICallback<Void, OSGIMetrics>(OSGIMetrics.class.getName()) {
            @Override
            public Void executeWithService(final OSGIMetrics service) {
                service.incrementCounter(counterName, step);
                return null;
            }
        });
    }

    @Override
    public void decrementCounter(final String counterName) {
        withServiceTracker(killbillTracker, new APICallback<Void, OSGIMetrics>(OSGIMetrics.class.getName()) {
            @Override
            public Void executeWithService(final OSGIMetrics service) {
                service.decrementCounter(counterName);
                return null;
            }
        });
    }

    @Override
    public void decrementCounter(final String counterName, final long step) {
        withServiceTracker(killbillTracker, new APICallback<Void, OSGIMetrics>(OSGIMetrics.class.getName()) {
            @Override
            public Void executeWithService(final OSGIMetrics service) {
                service.decrementCounter(counterName, step);
                return null;
            }
        });
    }
}

