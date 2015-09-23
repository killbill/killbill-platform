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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.killbill.billing.osgi.api.OSGIMetrics;

public class DefaultOSGIMetrics implements OSGIMetrics {
    private final MetricRegistry registry;
    private final String pluginName;

    public DefaultOSGIMetrics(final MetricRegistry registry, final String pluginName) {
        this.registry = registry;
        this.pluginName = pluginName;
    }

    @Override
    public void markMeter(final String meterName) {
        final Meter meter = registry.meter(fullMetricName(meterName));
        meter.mark();
    }

    @Override
    public void recordHistogramValue(final String histogramName, final long value) {
        final Histogram histogram = registry.histogram(fullMetricName(histogramName));
        histogram.update(value);
    }

    @Override
    public void incrementCounter(final String counterName) {
        incrementCounter(counterName, 1);
    }

    @Override
    public void incrementCounter(final String counterName, final long step) {
        final Counter counter = registry.counter(fullMetricName(counterName));
        counter.inc(step);
    }

    @Override
    public void decrementCounter(final String counterName) {
        decrementCounter(counterName, 1);
    }

    @Override
    public void decrementCounter(final String counterName, final long step) {
        final Counter counter = registry.counter(fullMetricName(counterName));
        counter.dec(step);
    }

    private String fullMetricName(final String metricName) {
        return pluginName + "." + metricName;
    }
}
