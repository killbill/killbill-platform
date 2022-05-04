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

package org.killbill.billing.osgi.bundles.prometheus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.killbill.commons.metrics.api.Counter;
import org.killbill.commons.metrics.api.Gauge;
import org.killbill.commons.metrics.api.Histogram;
import org.killbill.commons.metrics.api.Meter;
import org.killbill.commons.metrics.api.Metric;
import org.killbill.commons.metrics.api.MetricRegistry;
import org.killbill.commons.metrics.api.Snapshot;
import org.killbill.commons.metrics.api.Timer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.prometheus.client.Collector;

// Inspired from io.prometheus.client.dropwizard.DropwizardExports (Apache-2.0 License)
public class KillBillCollector extends Collector {

    private final MetricRegistry registry;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public KillBillCollector(final MetricRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        final Map<String, MetricFamilySamples> mfSamplesMap = new HashMap<>();

        for (final SortedMap.Entry<String, Gauge<?>> entry : registry.getGauges().entrySet()) {
            addToMap(mfSamplesMap, fromGauge(entry.getKey(), entry.getValue()));
        }
        for (final SortedMap.Entry<String, Counter> entry : registry.getCounters().entrySet()) {
            addToMap(mfSamplesMap, fromCounter(entry.getKey(), entry.getValue()));
        }
        for (final SortedMap.Entry<String, Histogram> entry : registry.getHistograms().entrySet()) {
            addToMap(mfSamplesMap, fromHistogram(entry.getKey(), entry.getValue()));
        }
        for (final SortedMap.Entry<String, Timer> entry : registry.getTimers().entrySet()) {
            addToMap(mfSamplesMap, fromTimer(entry.getKey(), entry.getValue()));
        }
        for (final SortedMap.Entry<String, Meter> entry : registry.getMeters().entrySet()) {
            addToMap(mfSamplesMap, fromMeter(entry.getKey(), entry.getValue()));
        }
        return new ArrayList<>(mfSamplesMap.values());
    }

    private MetricFamilySamples fromCounter(final String name, final Counter counter) {
        final MetricFamilySamples.Sample sample = createSample(name, "", new ArrayList<>(), new ArrayList<>(),
                                                               Long.valueOf(counter.getCount()).doubleValue());
        return new MetricFamilySamples(sample.name, Type.GAUGE, getHelpMessage(name, counter), List.of(sample));
    }

    private String getHelpMessage(final String metricName, final Metric metric) {
        return String.format("Generated from Kill Bill metric import (metric=%s, type=%s)",
                             metricName, metric.getClass().getName());
    }

    private MetricFamilySamples fromGauge(final String name, final Gauge<?> gauge) {
        final Object obj = gauge.getValue();
        final double value;
        if (obj instanceof Number) {
            value = ((Number) obj).doubleValue();
        } else if (obj instanceof Boolean) {
            value = ((Boolean) obj) ? 1 : 0;
        } else {
            return null;
        }
        final MetricFamilySamples.Sample sample = createSample(name, "",
                                                               new ArrayList<>(),
                                                               new ArrayList<>(),
                                                               value);
        return new MetricFamilySamples(sample.name, Type.GAUGE, getHelpMessage(name, gauge), List.of(sample));
    }

    private MetricFamilySamples fromSnapshotAndCount(final String name,
                                                     final Snapshot snapshot,
                                                     final long count,
                                                     final double factor,
                                                     final String helpMessage) {
        final List<MetricFamilySamples.Sample> samples = Arrays.asList(
                createSample(name, "", List.of("quantile"), List.of("0.5"), snapshot.getMedian() * factor),
                createSample(name, "", List.of("quantile"), List.of("0.75"), snapshot.get75thPercentile() * factor),
                createSample(name, "", List.of("quantile"), List.of("0.95"), snapshot.get95thPercentile() * factor),
                createSample(name, "", List.of("quantile"), List.of("0.98"), snapshot.get98thPercentile() * factor),
                createSample(name, "", List.of("quantile"), List.of("0.99"), snapshot.get99thPercentile() * factor),
                createSample(name, "", List.of("quantile"), List.of("0.999"), snapshot.get999thPercentile() * factor),
                createSample(name, "_count", new ArrayList<>(), new ArrayList<>(), count)
                                                                      );
        return new MetricFamilySamples(samples.get(0).name, Type.SUMMARY, helpMessage, samples);
    }

    private MetricFamilySamples fromHistogram(final String name, final Histogram histogram) {
        return fromSnapshotAndCount(name, histogram.getSnapshot(), histogram.getCount(), 1.0,
                                    getHelpMessage(name, histogram));
    }

    private MetricFamilySamples fromTimer(final String name, final Timer timer) {
        return fromSnapshotAndCount(name, timer.getSnapshot(), timer.getCount(),
                                    1.0D / TimeUnit.SECONDS.toNanos(1L), getHelpMessage(name, timer));
    }

    private MetricFamilySamples fromMeter(final String name, final Meter meter) {
        final MetricFamilySamples.Sample sample = createSample(name, "_total",
                                                               new ArrayList<>(),
                                                               new ArrayList<>(),
                                                               meter.getCount());
        return new MetricFamilySamples(sample.name, Type.COUNTER, getHelpMessage(name, meter),
                                       List.of(sample));
    }

    private void addToMap(final Map<String, MetricFamilySamples> mfSamplesMap, final MetricFamilySamples newMfSamples) {
        if (newMfSamples != null) {
            final MetricFamilySamples currentMfSamples = mfSamplesMap.get(newMfSamples.name);
            if (currentMfSamples == null) {
                mfSamplesMap.put(newMfSamples.name, newMfSamples);
            } else {
                final List<MetricFamilySamples.Sample> samples = new ArrayList<>(currentMfSamples.samples);
                samples.addAll(newMfSamples.samples);
                mfSamplesMap.put(newMfSamples.name, new MetricFamilySamples(newMfSamples.name, currentMfSamples.type, currentMfSamples.help, samples));
            }
        }
    }

    public Collector.MetricFamilySamples.Sample createSample(final String name,
                                                             final String nameSuffix,
                                                             final List<String> additionalLabelNames,
                                                             final List<String> additionalLabelValues,
                                                             final double value) {
        final String suffix = nameSuffix == null ? "" : nameSuffix;
        final List<String> labelNames = additionalLabelNames == null ? Collections.<String>emptyList() : additionalLabelNames;
        final List<String> labelValues = additionalLabelValues == null ? Collections.<String>emptyList() : additionalLabelValues;
        return new Collector.MetricFamilySamples.Sample(
                Collector.sanitizeMetricName(name + suffix),
                new ArrayList<>(labelNames),
                new ArrayList<>(labelValues),
                value
        );
    }
}
