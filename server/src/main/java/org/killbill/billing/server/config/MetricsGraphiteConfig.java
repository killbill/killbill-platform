/*
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
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

public interface MetricsGraphiteConfig extends KillbillPlatformConfig {

    @Config(KILL_BILL_NAMESPACE + "metrics.graphite")
    @Default("false")
    @Description("Whether metrics reporting to Graphite is enabled")
    public boolean isGraphiteReportingEnabled();

    @Config(KILL_BILL_NAMESPACE + "metrics.graphite.host")
    @Default("localhost")
    @Description("Graphite Hostname")
    public String getHostname();

    @Config(KILL_BILL_NAMESPACE + "metrics.graphite.port")
    @Default("2003")
    @Description("Graphite Port")
    public int getPort();

    @Config(KILL_BILL_NAMESPACE + "metrics.graphite.prefix")
    @Default("killbill")
    @Description("Prefix all metric names with the given string")
    public String getPrefix();

    @Config(KILL_BILL_NAMESPACE + "metrics.graphite.interval")
    @Default("30")
    @Description("Reporter polling interval in seconds")
    public int getInterval();
}
