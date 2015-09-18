package org.killbill.billing.osgi;

import com.codahale.metrics.MetricRegistry;
import org.killbill.billing.osgi.api.OSGIMetrics;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import javax.inject.Inject;

/**
 * Created by arodrigues on 9/17/15.
 */
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
