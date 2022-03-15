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

package org.killbill.billing.server.metrics;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.killbill.billing.osgi.api.OSGISingleServiceRegistration;
import org.killbill.commons.metrics.api.Counter;
import org.killbill.commons.metrics.api.Gauge;
import org.killbill.commons.metrics.api.Histogram;
import org.killbill.commons.metrics.api.Meter;
import org.killbill.commons.metrics.api.MetricRegistry;
import org.killbill.commons.metrics.api.Snapshot;
import org.killbill.commons.metrics.api.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KillbillPluginsMetricRegistry implements MetricRegistry {

    private static final Logger logger = LoggerFactory.getLogger(KillbillPluginsMetricRegistry.class);

    private final OSGISingleServiceRegistration<MetricRegistry> pluginMetricRegistry;

    public KillbillPluginsMetricRegistry(final OSGISingleServiceRegistration<MetricRegistry> pluginMetricRegistry) {
        this.pluginMetricRegistry = pluginMetricRegistry;
    }

    @Override
    public Counter counter(final String name) {
        return new Counter() {
            @Override
            public void inc(final long n) {
                final MetricRegistry service = pluginMetricRegistry.getService();
                if (service != null) {
                    service.counter(name).inc(n);
                }
            }

            @Override
            public long getCount() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.counter(name).getCount() : 0;
            }
        };
    }

    @Override
    public <T> Gauge<T> gauge(final String name, final Gauge<T> gauge) {
        // Unlike other metrics, Gauges are usually created once and callers don't keep a reference to it
        pluginMetricRegistry.addRegistrationListener(new Runnable() {
            @Override
            public void run() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                if (service != null) {
                    service.gauge(name, gauge);
                } // else: race condition? We should be picked up again
            }
        });
        return new Gauge<T>() {
            @Override
            public T getValue() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.gauge(name, gauge).getValue() : null;
            }
        };
    }

    @Override
    public Histogram histogram(final String name) {
        return new Histogram() {
            @Override
            public void update(final long value) {
                final MetricRegistry service = pluginMetricRegistry.getService();
                if (service != null) {
                    service.histogram(name).update(value);
                }
            }

            @Override
            public long getCount() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.histogram(name).getCount() : 0;
            }

            @Override
            public Snapshot getSnapshot() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.histogram(name).getSnapshot() : null;
            }
        };
    }

    @Override
    public Meter meter(final String name) {
        return new Meter() {
            @Override
            public void mark(final long n) {
                final MetricRegistry service = pluginMetricRegistry.getService();
                if (service != null) {
                    service.meter(name).mark(n);
                }
            }

            @Override
            public double getFifteenMinuteRate() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.meter(name).getFifteenMinuteRate() : 0;
            }

            @Override
            public double getFiveMinuteRate() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.meter(name).getFiveMinuteRate() : 0;
            }

            @Override
            public double getMeanRate() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.meter(name).getMeanRate() : 0;
            }

            @Override
            public double getOneMinuteRate() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.meter(name).getOneMinuteRate() : 0;
            }

            @Override
            public long getCount() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.meter(name).getCount() : 0;
            }
        };
    }

    @Override
    public Timer timer(final String name) {
        return new Timer() {
            @Override
            public long getCount() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.timer(name).getCount() : 0;
            }

            @Override
            public void update(final long duration, final TimeUnit unit) {
                final MetricRegistry service = pluginMetricRegistry.getService();
                if (service != null) {
                    service.timer(name).update(duration, unit);
                }
            }

            @Override
            public double getFifteenMinuteRate() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.timer(name).getFifteenMinuteRate() : 0;
            }

            @Override
            public double getFiveMinuteRate() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.timer(name).getFiveMinuteRate() : 0;
            }

            @Override
            public double getMeanRate() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.timer(name).getMeanRate() : 0;
            }

            @Override
            public double getOneMinuteRate() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.timer(name).getOneMinuteRate() : 0;
            }

            @Override
            public Snapshot getSnapshot() {
                final MetricRegistry service = pluginMetricRegistry.getService();
                return service != null ? service.timer(name).getSnapshot() : null;
            }
        };
    }

    @Override
    public boolean remove(final String name) {
        final MetricRegistry service = pluginMetricRegistry.getService();
        return service != null && service.remove(name);
    }

    @Override
    public Map<String, ?> getMetrics() {
        final MetricRegistry service = pluginMetricRegistry.getService();
        return service != null ? service.getMetrics() : Collections.emptyMap();
    }

    @Override
    public Map<String, Counter> getCounters() {
        final MetricRegistry service = pluginMetricRegistry.getService();
        return service != null ? service.getCounters() : Collections.emptyMap();
    }

    @Override
    public Map<String, Histogram> getHistograms() {
        final MetricRegistry service = pluginMetricRegistry.getService();
        return service != null ? service.getHistograms() : Collections.emptyMap();
    }

    @Override
    public Map<String, Gauge<?>> getGauges() {
        final MetricRegistry service = pluginMetricRegistry.getService();
        return service != null ? service.getGauges() : Collections.emptyMap();
    }

    @Override
    public Map<String, Meter> getMeters() {
        final MetricRegistry service = pluginMetricRegistry.getService();
        return service != null ? service.getMeters() : Collections.emptyMap();
    }

    @Override
    public Map<String, Timer> getTimers() {
        final MetricRegistry service = pluginMetricRegistry.getService();
        return service != null ? service.getTimers() : Collections.emptyMap();
    }
}
