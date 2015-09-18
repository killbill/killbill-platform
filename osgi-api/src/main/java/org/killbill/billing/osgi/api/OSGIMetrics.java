package org.killbill.billing.osgi.api;

/**
 * Created by arodrigues on 9/17/15.
 */
public interface OSGIMetrics {
    void markMeter(String meterName);

    void recordHistogramValue(String histogramName, long value);

    void incrementCounter(String counterName);

    void incrementCounter(String counterName, long step);

    void decrementCounter(String counterName);

    void decrementCounter(String counterName, long step);
}
