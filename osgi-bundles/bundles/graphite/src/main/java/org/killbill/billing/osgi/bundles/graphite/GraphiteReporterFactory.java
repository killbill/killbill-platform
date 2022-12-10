/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
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

package org.killbill.billing.osgi.bundles.graphite;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import io.dropwizard.metrics.BaseReporterFactory;

public class GraphiteReporterFactory extends BaseReporterFactory {

    private String prefix;
    private Graphite graphite;

    public GraphiteReporterFactory setPrefix(final String prefix) {
        this.prefix = prefix;
        return this;
    }

    public GraphiteReporterFactory setGraphite(final Graphite graphite) {
        this.graphite = graphite;
        return this;
    }

    @Override
    public ScheduledReporter build(final MetricRegistry metricRegistry) {
        return GraphiteReporter.forRegistry(metricRegistry)
                               .prefixedWith(prefix)
                               .convertRatesTo(TimeUnit.SECONDS)
                               .convertDurationsTo(TimeUnit.NANOSECONDS)
                               .filter(MetricFilter.ALL)
                               .build(graphite);
    }
}
