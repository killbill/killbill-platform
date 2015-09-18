package org.killbill.killbill.osgi.libs.killbill;

import org.killbill.billing.osgi.api.OSGIMetrics;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Created by arodrigues on 9/17/15.
 */
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

