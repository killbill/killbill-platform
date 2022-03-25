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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Predicate;
import io.prometheus.client.SampleNameFilter;
import io.prometheus.client.exporter.common.TextFormat;

// We cannot use io.prometheus.client.servlet.common.exporter.Exporter as it puts the Response in Writer mode
// while Kill Bill expects it to be in Streaming mode
public class KillBillExporter {

    private final CollectorRegistry registry;
    private final Predicate<String> sampleNameFilter;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public KillBillExporter(final CollectorRegistry registry, final Predicate<String> sampleNameFilter) {
        this.registry = registry;
        this.sampleNameFilter = sampleNameFilter;
    }

    public void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        resp.setStatus(200);
        final String contentType = TextFormat.chooseContentType(req.getHeader("Accept"));
        resp.setContentType(contentType);

        try (final Writer writer = new BufferedWriter(new OutputStreamWriter(resp.getOutputStream(), StandardCharsets.UTF_8))) {
            final Predicate<String> filter = SampleNameFilter.restrictToNamesEqualTo(this.sampleNameFilter, parse(req));
            if (filter == null) {
                TextFormat.writeFormat(contentType, writer, this.registry.metricFamilySamples());
            } else {
                TextFormat.writeFormat(contentType, writer, this.registry.filteredMetricFamilySamples(filter));
            }

            writer.flush();
        }
    }

    private Set<String> parse(final ServletRequest req) {
        final String[] includedParam = req.getParameterValues("name[]");
        return includedParam == null ? Collections.emptySet() : new HashSet(Arrays.asList(includedParam));
    }
}
