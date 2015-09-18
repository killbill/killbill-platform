package org.killbill.billing.osgi;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.killbill.billing.osgi.api.OSGIMetrics;

/**
 * Created by arodrigues on 9/17/15.
 */
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
