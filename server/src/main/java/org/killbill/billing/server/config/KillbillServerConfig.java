/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014-2017 Groupon, Inc
 * Copyright 2014-2017 The Billing Project, LLC
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
import org.skife.config.TimeSpan;

public interface KillbillServerConfig extends KillbillPlatformConfig {

    @Config(KILL_BILL_NAMESPACE + "server.multitenant")
    @Default("true")
    @Description("Whether multi-tenancy is enabled")
    public boolean isMultiTenancyEnabled();

    @Config(KILL_BILL_NAMESPACE + "server.test.mode")
    @Default("false")
    @Description("Whether to start in test mode")
    public boolean isTestModeEnabled();

    @Config(KILL_BILL_NAMESPACE + "server.test.clock.redis")
    @Default("false")
    @Description("Whether Redis integration for the clock is enabled")
    public boolean isRedisClockEnabled();

    @Config(KILL_BILL_NAMESPACE + "server.test.clock.redis.url")
    @Default("redis://127.0.0.1:6379")
    @Description("Redis clock URL")
    public String getUrl();

    @Config(KILL_BILL_NAMESPACE + "server.test.clock.redis.connectionMinimumIdleSize")
    @Default("1")
    @Description("Minimum number of connections for the Redis clock")
    public int getConnectionMinimumIdleSize();

    @Config(KILL_BILL_NAMESPACE + "server.baseUrl")
    @Default("http://127.0.0.1:8080")
    @Description("Server base url")
    public String getBaseUrl();

    @Config(KILL_BILL_NAMESPACE + "server.region")
    @Default("local")
    @Description("Region or data center where the server is deployed")
    public String getRegion();

    @Config(KILL_BILL_NAMESPACE + "server.http.gzip")
    @Default("false")
    @Description("Allow Kill Bill to return gzip json when Content-Encoding is set with gzip")
    public boolean isConfiguredToReturnGZIPResponses();

    @Config(KILL_BILL_NAMESPACE + "server.shutdownDelay")
    @Default("0s")
    @Description("Shutdown delay before starting shutdown sequence")
    public TimeSpan getShutdownDelay();
}
