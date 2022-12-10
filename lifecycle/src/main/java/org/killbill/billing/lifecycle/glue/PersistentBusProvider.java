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

package org.killbill.billing.lifecycle.glue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.killbill.bus.DefaultPersistentBus;
import org.killbill.bus.InMemoryPersistentBus;
import org.killbill.bus.api.PersistentBus;
import org.killbill.bus.api.PersistentBusConfig;
import org.killbill.clock.Clock;
import org.killbill.commons.jdbi.notification.DatabaseTransactionNotificationApi;
import org.killbill.commons.metrics.api.MetricRegistry;
import org.killbill.queue.DefaultQueueLifecycle;
import org.skife.jdbi.v2.IDBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentBusProvider implements Provider<PersistentBus> {

    private static final Logger logger = LoggerFactory.getLogger(PersistentBusProvider.class);

    private final PersistentBusConfig busConfig;

    private IDBI dbi;
    private Clock clock;
    private MetricRegistry metricRegistry;
    private DatabaseTransactionNotificationApi databaseTransactionNotificationApi;

    public PersistentBusProvider(final PersistentBusConfig busConfig) {
        this.busConfig = busConfig;
    }

    @Inject
    public void initialize(@Named(DefaultQueueLifecycle.QUEUE_NAME) final IDBI dbi,
                           final DatabaseTransactionNotificationApi observable,
                           final Clock clock,
                           final MetricRegistry metricRegistry) {
        this.dbi = dbi;
        this.clock = clock;
        this.metricRegistry = metricRegistry;
        this.databaseTransactionNotificationApi = observable;
    }

    @Override
    public PersistentBus get() {
        if (busConfig.isInMemory()) {
            logger.info("Creating InMemory bus for " + busConfig.getTableName());
            return new InMemoryPersistentBus(busConfig);
        } else {
            logger.info("Creating Persistent bus for " + busConfig.getTableName());
            return new DefaultPersistentBus(dbi, clock, busConfig, metricRegistry, databaseTransactionNotificationApi);
        }
    }
}
