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

package org.killbill.billing.osgi.bundles.metrics;

import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.MBeanServer;

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.commons.health.api.HealthCheck;
import org.killbill.commons.health.api.Result;
import org.killbill.commons.health.impl.HealthyResultBuilder;
import org.killbill.commons.health.impl.UnhealthyResultBuilder;
import org.killbill.commons.metrics.dropwizard.KillBillCodahaleMetricRegistry;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadDeadlockDetector;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

public class Activator extends KillbillActivatorBase {

    public static final String BUNDLE_NAME = "killbill-metrics";
    private static final Logger logger = LoggerFactory.getLogger(Activator.class);
    private JmxReporter metricsJMXReporter;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        // Create the underlying registry
        final MetricRegistry metricRegistry = new MetricRegistry();

        // Expose the registry to Kill Bill and other plugins
        registerMetricRegistry(context, new KillBillCodahaleMetricRegistry(metricRegistry));

        // Instrument the JVM (via JMX)
        final MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        registerAll("buffers", new BufferPoolMetricSet(platformMBeanServer), metricRegistry);
        registerAll("classloading", new ClassLoadingGaugeSet(), metricRegistry);
        registerAll("gc", new GarbageCollectorMetricSet(), metricRegistry);
        registerAll("memory", new MemoryUsageGaugeSet(), metricRegistry);
        registerAll("threads", new ThreadStatesGaugeSet(), metricRegistry);

        // Expose metrics via JMX
        logger.info("Reporting metrics to JMX");
        metricsJMXReporter = JmxReporter.forRegistry(metricRegistry).registerWith(platformMBeanServer).build();
        metricsJMXReporter.start();

        final ThreadDeadlockDetector detector = new ThreadDeadlockDetector();
        registerHealthcheck(context, new HealthCheck() {
            @Override
            public Result check() throws Exception {
                final Set<String> threads = detector.getDeadlockedThreads();
                if (threads.isEmpty()) {
                    return new HealthyResultBuilder().createHealthyResult();
                } else {
                    return new UnhealthyResultBuilder().setMessage(threads.toString()).createUnhealthyResult();
                }
            }
        });
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        try {
            if (metricsJMXReporter != null) {
                metricsJMXReporter.stop();
            }
        } finally {
            super.stop(context);
        }
    }

    private void registerAll(final String prefix, final MetricSet metricSet, final MetricRegistry registry) {
        for (final Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            if (entry.getValue() instanceof MetricSet) {
                registerAll(prefix + "." + entry.getKey(), (MetricSet) entry.getValue(), registry);
            } else {
                registry.register(prefix + "." + entry.getKey(), entry.getValue());
            }
        }
    }

    private void registerMetricRegistry(final BundleContext context, final org.killbill.commons.metrics.api.MetricRegistry killbillMetricRegistry) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, BUNDLE_NAME);
        registrar.registerService(context, org.killbill.commons.metrics.api.MetricRegistry.class, killbillMetricRegistry, props);
    }

    private void registerHealthcheck(final BundleContext context, final HealthCheck healthcheck) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, BUNDLE_NAME);
        registrar.registerService(context, HealthCheck.class, healthcheck, props);
    }
}
