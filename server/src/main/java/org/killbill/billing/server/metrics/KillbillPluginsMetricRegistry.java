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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.killbill.billing.osgi.api.OSGIServiceRegistration;
import org.killbill.commons.metrics.api.Counter;
import org.killbill.commons.metrics.api.Gauge;
import org.killbill.commons.metrics.api.Histogram;
import org.killbill.commons.metrics.api.Meter;
import org.killbill.commons.metrics.api.Metric;
import org.killbill.commons.metrics.api.MetricRegistry;
import org.killbill.commons.metrics.api.Snapshot;
import org.killbill.commons.metrics.api.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KillbillPluginsMetricRegistry implements MetricRegistry {

    private static final Logger logger = LoggerFactory.getLogger(KillbillPluginsMetricRegistry.class);

    private final OSGIServiceRegistration<MetricRegistry> pluginMetricRegistries;

    public KillbillPluginsMetricRegistry(final OSGIServiceRegistration<MetricRegistry> pluginMetricRegistries) {
        this.pluginMetricRegistries = pluginMetricRegistries;
    }

    @Override
    public Counter counter(final String name) {
        return new Counter() {
            @Override
            public void inc(final long n) {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    pluginMetricRegistry.counter(name).inc(n);
                }
            }

            @Override
            public long getCount() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.counter(name).getCount();
                }
                return 0;
            }
        };
    }

    @Override
    public <T> Gauge<T> gauge(final String name) {
        return new Gauge<T>() {
            @Override
            public T getValue() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // TODO Should be SettableGauge instead?
                    //pluginMetricRegistry.gauge(name).inc(n);
                }
                return null;
            }
        };
    }

    @Override
    public Histogram histogram(final String name) {
        return new Histogram() {
            @Override
            public void update(final long value) {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    pluginMetricRegistry.histogram(name).update(value);
                }
            }

            @Override
            public long getCount() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.histogram(name).getCount();
                }
                return 0;
            }

            @Override
            public Snapshot getSnapshot() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.histogram(name).getSnapshot();
                }
                return null;
            }
        };
    }

    @Override
    public Meter meter(final String name) {
        return new Meter() {
            @Override
            public void mark(final long n) {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    pluginMetricRegistry.meter(name).mark(n);
                }
            }

            @Override
            public double getFifteenMinuteRate() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.meter(name).getFifteenMinuteRate();
                }
                return 0;
            }

            @Override
            public double getFiveMinuteRate() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.meter(name).getFiveMinuteRate();
                }
                return 0;
            }

            @Override
            public double getMeanRate() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.meter(name).getMeanRate();
                }
                return 0;
            }

            @Override
            public double getOneMinuteRate() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.meter(name).getOneMinuteRate();
                }
                return 0;
            }

            @Override
            public long getCount() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.meter(name).getCount();
                }
                return 0;
            }
        };
    }

    @Override
    public Timer timer(final String name) {
        return new Timer() {
            @Override
            public long getCount() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.meter(name).getCount();
                }
                return 0;
            }

            @Override
            public void update(final long duration, final TimeUnit unit) {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    pluginMetricRegistry.timer(name).update(duration, unit);
                }
            }

            @Override
            public double getFifteenMinuteRate() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.timer(name).getFifteenMinuteRate();
                }
                return 0;
            }

            @Override
            public double getFiveMinuteRate() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.timer(name).getFiveMinuteRate();
                }
                return 0;
            }

            @Override
            public double getMeanRate() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.timer(name).getMeanRate();
                }
                return 0;
            }

            @Override
            public double getOneMinuteRate() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.timer(name).getOneMinuteRate();
                }
                return 0;
            }

            @Override
            public Snapshot getSnapshot() {
                for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
                    final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
                    // Return the value of the first one (arbitrary)
                    return pluginMetricRegistry.timer(name).getSnapshot();
                }
                return null;
            }
        };
    }

    @Override
    public <T extends Metric> T register(final String name, final T metric) {
        for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
            final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
            pluginMetricRegistry.register(name, metric);
        }
        return metric;
    }

    @Override
    public boolean remove(final String name) {
        boolean removed = false;
        for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
            final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
            removed = pluginMetricRegistry.remove(name) || removed;
        }
        return removed;
    }

    @Override
    public Map<String, ?> getMetrics() {
        final Map<String, Object> metrics = new HashMap<>();
        for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
            final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
            metrics.putAll(pluginMetricRegistry.getMetrics());
        }
        return metrics;
    }

    @Override
    public Map<String, Counter> getCounters() {
        final Map<String, Counter> counters = new HashMap<>();
        for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
            final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
            counters.putAll(pluginMetricRegistry.getCounters());
        }
        return counters;
    }

    @Override
    public Map<String, Histogram> getHistograms() {
        final Map<String, Histogram> histograms = new HashMap<>();
        for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
            final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
            histograms.putAll(pluginMetricRegistry.getHistograms());
        }
        return histograms;
    }

    @Override
    public Map<String, Gauge<?>> getGauges() {
        final Map<String, Gauge<?>> gauges = new HashMap<>();
        for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
            final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
            gauges.putAll(pluginMetricRegistry.getGauges());
        }
        return gauges;
    }

    @Override
    public Map<String, Meter> getMeters() {
        final Map<String, Meter> meters = new HashMap<>();
        for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
            final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
            meters.putAll(pluginMetricRegistry.getMeters());
        }
        return meters;
    }

    @Override
    public Map<String, Timer> getTimers() {
        final Map<String, Timer> timers = new HashMap<>();
        for (final String pluginMetricRegistryService : pluginMetricRegistries.getAllServices()) {
            final MetricRegistry pluginMetricRegistry = pluginMetricRegistries.getServiceForName(pluginMetricRegistryService);
            timers.putAll(pluginMetricRegistry.getTimers());
        }
        return timers;
    }
}
