/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

package org.killbill.billing.server.config;

import org.killbill.billing.platform.api.KillbillPlatformConfig;
import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.Description;

import com.izettle.metrics.dw.SenderType;

public interface MetricsInfluxDbConfig extends KillbillPlatformConfig {

    @Config(KILL_BILL_NAMESPACE + "metrics.influxDb")
    @Default("false")
    @Description("Whether metrics reporting to InfluxDB is enabled")
    public boolean isInfluxDbReportingEnabled();

    @Config(KILL_BILL_NAMESPACE + "metrics.influxDb.senderType")
    @Default("HTTP")
    @Description("InfluxDb protocol")
    public SenderType getSenderType();

    @Config(KILL_BILL_NAMESPACE + "metrics.influxDb.host")
    @Default("localhost")
    @Description("InfluxDb Hostname")
    public String getHostname();

    @Config(KILL_BILL_NAMESPACE + "metrics.influxDb.port")
    @Default("2003")
    @Description("InfluxDb Port")
    public int getPort();

    @Config(KILL_BILL_NAMESPACE + "metrics.influxDb.socketTimeout")
    @Default("1000")
    @Description("InfluxDb socket timeout")
    public int getSocketTimeout();

    @Config(KILL_BILL_NAMESPACE + "metrics.influxDb.database")
    @Default("killbill")
    @Description("InfluxDb database")
    public String getDatabase();

    @Config(KILL_BILL_NAMESPACE + "metrics.influxDb.prefix")
    @Default("killbill")
    @Description("Prefix all metric names with the given string")
    public String getPrefix();

    @Config(KILL_BILL_NAMESPACE + "metrics.influxDb.interval")
    @Default("30")
    @Description("Reporter polling interval in seconds")
    public int getInterval();
}
