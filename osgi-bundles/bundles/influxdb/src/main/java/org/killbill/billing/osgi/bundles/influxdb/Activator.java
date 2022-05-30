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

package org.killbill.billing.osgi.bundles.influxdb;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.commons.metrics.dropwizard.CodahaleMetricRegistry;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ScheduledReporter;
import com.google.common.base.MoreObjects;
import com.izettle.metrics.dw.SenderType;
import io.dropwizard.util.Duration;

public class Activator extends KillbillActivatorBase {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    private static final String KILL_BILL_NAMESPACE = "org.killbill.";

    private ScheduledReporter scheduledReporter;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final boolean influxDBEnabled = "true".equals(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.influxDb"), "false"));
        if (influxDBEnabled) {
            // Stream metric values to a InfluxDB server
            final CustomInfluxDbReporterFactory influxDbReporterFactory = new CustomInfluxDbReporterFactory();
            influxDbReporterFactory.setRateUnit(TimeUnit.SECONDS);
            influxDbReporterFactory.setDurationUnit(TimeUnit.NANOSECONDS);
            influxDbReporterFactory.setHost(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.influxDb.host"), "localhost"));
            influxDbReporterFactory.setPort(Integer.parseInt(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.influxDb.port"), "8086")));
            influxDbReporterFactory.setReadTimeout(Integer.parseInt(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.influxDb.socketTimeout"), "1000")));
            influxDbReporterFactory.setDatabase(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.influxDb.database"), "killbill"));
            influxDbReporterFactory.setPrefix(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.influxDb.prefix"), ""));
            influxDbReporterFactory.setSenderType(SenderType.valueOf(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.influxDb.senderType"), "HTTP")));
            influxDbReporterFactory.setOrganization(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.influxDb.organization"), "killbill"));
            influxDbReporterFactory.setBucket(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.influxDb.bucket"), "killbill"));
            influxDbReporterFactory.setToken(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.influxDb.token"), ""));

            final int reportingFrequency = Integer.parseInt(MoreObjects.firstNonNull(configProperties.getString(KILL_BILL_NAMESPACE + "metrics.influxDb.interval"), "30"));
            influxDbReporterFactory.setFrequency(Optional.of(Duration.seconds(reportingFrequency)));

            logger.info("Reporting metrics to InfluxDB {}:{}", influxDbReporterFactory.getHost(), influxDbReporterFactory.getPort());
            scheduledReporter = influxDbReporterFactory.build(new CodahaleMetricRegistry(this.metricRegistry.getMetricRegistry()));

            logger.info("Starts the InfluxDB reporter polling at the interval of {} seconds", reportingFrequency);

            scheduledReporter.start(reportingFrequency, TimeUnit.SECONDS);
        } else {
            logger.info("Reporting to InfluxDB disabled");
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
