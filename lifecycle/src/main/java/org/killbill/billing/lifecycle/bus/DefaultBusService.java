/*
 * Copyright 2010-2011 Ning, Inc.
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
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

import org.killbill.billing.lifecycle.api.BusService;
import org.killbill.billing.platform.api.LifecycleHandlerType;
import org.killbill.bus.api.PersistentBus;

import com.google.inject.Inject;

public class DefaultBusService implements BusService {

    public static final String EVENT_BUS_GROUP_NAME = "bus-grp";
    public static final String EVENT_BUS_TH_NAME = "bus-th";

    public static final String EVENT_BUS_SERVICE = "bus-service";
    public static final String EVENT_BUS_IDENTIFIER = EVENT_BUS_SERVICE;

    private final PersistentBus eventBus;

    @Inject
    public DefaultBusService(final PersistentBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String getName() {
        return EVENT_BUS_SERVICE;
    }

    @LifecycleHandlerType(LifecycleHandlerType.LifecycleLevel.INIT_BUS)
    public void startBus() {
        eventBus.start();
    }

    @LifecycleHandlerType(LifecycleHandlerType.LifecycleLevel.STOP_BUS)
    public void stopBus() {
        eventBus.stop();
    }

    @Override
    public PersistentBus getBus() {
        return eventBus;
    }
}
