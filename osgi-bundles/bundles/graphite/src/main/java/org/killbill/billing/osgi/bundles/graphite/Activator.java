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

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.commons.metrics.dropwizard.CodahaleMetricRegistry;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.graphite.Graphite;
import com.google.common.base.MoreObjects;
import io.dropwizard.util.Duration;

public class Activator extends KillbillActivatorBase {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    private static final String KILL_BILL_NAMESPACE = "org.killbill.";

    private ScheduledReporter scheduledReporter;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final boolean graphiteEnabled = "true".equals(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.graphite"), "false"));
        if (graphiteEnabled) {
            // Stream metric values to a Graphite server
            final InetSocketAddress address = new InetSocketAddress(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.graphite.host"), "localhost"),
                                                                    Integer.parseInt(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.graphite.port"), "2003")));
            final Graphite graphite = new Graphite(address);

            final int reportingFrequency = Integer.parseInt(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.graphite.interval"), "30"));

            final GraphiteReporterFactory reporterFactory = new GraphiteReporterFactory().setPrefix(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.graphite.prefix"), "killbill"))
                                                                                         .setGraphite(graphite);
            reporterFactory.setFrequency(Optional.of(Duration.seconds(reportingFrequency)));

            logger.info("Reporting metrics to Graphite {}", address);

            scheduledReporter = reporterFactory.build(new CodahaleMetricRegistry(this.metricRegistry.getMetricRegistry()));

            logger.info("Starts the reporter polling at the interval of {} seconds", reportingFrequency);

            scheduledReporter.start(reportingFrequency, TimeUnit.SECONDS);
        } else {
            logger.info("Reporting metrics to Graphite disabled");
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        try {
            if (scheduledReporter != null) {
                scheduledReporter.stop();
            }
        } finally {
            super.stop(context);
        }
    }
}
