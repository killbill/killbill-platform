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

package org.killbill.billing.server.modules;

import javax.inject.Inject;
import javax.inject.Provider;

import org.killbill.billing.server.config.KillbillServerConfig;
import org.killbill.clock.DistributedClockMock;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class DistributedClockProvider implements Provider<DistributedClockMock> {

    private final KillbillServerConfig serverConfig;

    @Inject
    public DistributedClockProvider(final KillbillServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public DistributedClockMock get() {
        final Config redissonCfg = new Config();
        redissonCfg.useSingleServer()
                   .setAddress(serverConfig.getUrl())
                   .setConnectionMinimumIdleSize(serverConfig.getConnectionMinimumIdleSize());
        final RedissonClient redissonClient = Redisson.create(redissonCfg);

        final DistributedClockMock distributedClockMock = new DistributedClockMock();
        distributedClockMock.setRedissonClient(redissonClient);

        return distributedClockMock;
    }
}
