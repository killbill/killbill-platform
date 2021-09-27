/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2021 Equinix, Inc
 * Copyright 2014-2021 The Billing Project, LLC
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

package org.killbill.billing.lifecycle.bus;

import org.killbill.bus.api.PersistentBusConfig;
import org.skife.config.ConfigSource;
import org.skife.config.ConfigurationObjectFactory;
import org.skife.config.TimeSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

// Hack to make sure the external and internal buses don't share the same tables names.
// See discussion https://groups.google.com/forum/#!msg/killbilling-users/x3o1-EjR3V0/ZJ-PJYFM_M0J
public class ExternalPersistentBusConfig extends PersistentBusConfig {

    private static final Logger logger = LoggerFactory.getLogger(ExternalPersistentBusConfig.class);

    public static final String MAIN_BUS_NAME = "main";
    public static final String EXTERNAL_BUS_NAME = "external";

    private static final String TABLE_NAME_DEFAULT_VALUE = "bus_events";
    private static final String TABLE_NAME_ALTERNATE_DEFAULT_VALUE = "bus_ext_events";

    private static final String HISTORY_TABLE_NAME_DEFAULT_VALUE = "bus_events_history";
    private static final String HISTORY_TABLE_NAME_ALTERNATE_DEFAULT_VALUE = "bus_ext_events_history";

    private final PersistentBusConfig internalPersistentBusConfig;
    private final PersistentBusConfig externalPersistentBusConfig;

    public ExternalPersistentBusConfig(final ConfigSource configSource) {
        // See org.killbill.billing.util.glue.BusModule
        internalPersistentBusConfig = new ConfigurationObjectFactory(configSource).buildWithReplacements(PersistentBusConfig.class,
                                                                                                         ImmutableMap.<String, String>of("instanceName", MAIN_BUS_NAME));
        externalPersistentBusConfig = new ConfigurationObjectFactory(configSource).buildWithReplacements(PersistentBusConfig.class,
                                                                                                         ImmutableMap.<String, String>of("instanceName", EXTERNAL_BUS_NAME));
    }

    @Override
    public boolean isInMemory() {
        return externalPersistentBusConfig.isInMemory();
    }

    @Override
    public int getMaxFailureRetries() {
        return externalPersistentBusConfig.getMaxFailureRetries();
    }

    @Override
    public int getMinInFlightEntries() {
        return 1;
    }

    @Override
    public int getMaxInFlightEntries() {
        return 100;
    }

    @Override
    public int getMaxEntriesClaimed() {
        return externalPersistentBusConfig.getMaxEntriesClaimed();
    }

    @Override
    public PersistentQueueMode getPersistentQueueMode() {
        return externalPersistentBusConfig.getPersistentQueueMode();
    }

    @Override
    public TimeSpan getClaimedTime() {
        return externalPersistentBusConfig.getClaimedTime();
    }

    @Override
    public long getPollingSleepTimeMs() {
        return externalPersistentBusConfig.getPollingSleepTimeMs();
    }

    @Override
    public boolean isProcessingOff() {
        return externalPersistentBusConfig.isProcessingOff();
    }

    @Override
    public int geMaxDispatchThreads() {
        return externalPersistentBusConfig.geMaxDispatchThreads();
    }

    @Override
    public int geNbLifecycleDispatchThreads() {
        return 1;
    }

    @Override
    public int geNbLifecycleCompleteThreads() {
        return 1;
    }

    @Override
    public int getEventQueueCapacity() {
        return 0;
    }

    @Override
    public String getTableName() {
        if (internalPersistentBusConfig.getTableName().equals(externalPersistentBusConfig.getTableName())) {
            if (TABLE_NAME_DEFAULT_VALUE.equals(externalPersistentBusConfig.getTableName())) {
                logger.debug("Overriding default value for the external bus table name");
                return TABLE_NAME_ALTERNATE_DEFAULT_VALUE;
            } else {
                // Overridden by the user?
                throw new IllegalArgumentException("The external and internal buses cannot share the same table name " + externalPersistentBusConfig.getTableName());
            }
        } else {
            return externalPersistentBusConfig.getTableName();
        }
    }

    @Override
    public String getHistoryTableName() {
        if (internalPersistentBusConfig.getHistoryTableName().equals(externalPersistentBusConfig.getHistoryTableName())) {
            if (HISTORY_TABLE_NAME_DEFAULT_VALUE.equals(externalPersistentBusConfig.getHistoryTableName())) {
                logger.debug("Overriding default value for the external bus history table name");
                return HISTORY_TABLE_NAME_ALTERNATE_DEFAULT_VALUE;
            } else {
                // Overridden by the user?
                throw new IllegalArgumentException("The external and internal buses cannot share the same history table name " + externalPersistentBusConfig.getHistoryTableName());
            }
        } else {
            return externalPersistentBusConfig.getHistoryTableName();
        }
    }

    @Override
    public TimeSpan getReapThreshold() {
        return externalPersistentBusConfig.getReapThreshold();
    }

    @Override
    public int getMaxReDispatchCount() {
        return externalPersistentBusConfig.getMaxReDispatchCount();
    }

    @Override
    public TimeSpan getReapSchedule() {
        return externalPersistentBusConfig.getReapSchedule();
    }

    @Override
    public TimeSpan getShutdownTimeout() {
        return externalPersistentBusConfig.getShutdownTimeout();
    }
}
