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

package org.killbill.billing.osgi.bundles.influxdb;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.izettle.metrics.dw.InfluxDbReporterFactory;
import com.izettle.metrics.influxdb.InfluxDbReporter.Builder;

public class CustomInfluxDbReporterFactory extends InfluxDbReporterFactory {

    @Override
    public ScheduledReporter build(final MetricRegistry registry) {
        try {
            Builder builder = this.builder(registry);
            switch (this.getSenderType()) {
                case HTTP:
                    return builder.build(new CustomInfluxDbHttpSender(this.getProtocol(), this.getHost(), this.getPort(), this.getDatabase(),
                                                                      this.getAuth(), this.getPrecision().getUnit(), this.getConnectTimeout(),
                                                                      this.getReadTimeout(), this.getPrefix()));
                default:
                    throw new UnsupportedOperationException("The Sender Type is not supported. ");
            }
        } catch (Exception var3) {
            throw new RuntimeException(var3);
        }
    }
}