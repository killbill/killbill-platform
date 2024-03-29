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

package org.killbill.billing.lifecycle.bus;

import javax.inject.Inject;

import org.killbill.billing.lifecycle.api.BusService;
import org.killbill.billing.platform.api.LifecycleHandlerType;
import org.killbill.billing.platform.api.LifecycleHandlerType.LifecycleLevel;
import org.killbill.bus.api.PersistentBus;

public class DefaultBusService implements BusService {


    private final PersistentBus eventBus;

    @Inject
    public DefaultBusService(final PersistentBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String getName() {
        return KILLBILL_SERVICES.BUS_SERVICE.getServiceName() ;
    }

    @Override
    public int getRegistrationOrdering() {
        return KILLBILL_SERVICES.BUS_SERVICE.getRegistrationOrdering();
    }

    @LifecycleHandlerType(LifecycleHandlerType.LifecycleLevel.INIT_BUS)
    public void initBus() {
        eventBus.initQueue();
    }

    @LifecycleHandlerType(LifecycleLevel.START_BUS)
    public void startBus() {
        eventBus.startQueue();
    }

    @LifecycleHandlerType(LifecycleHandlerType.LifecycleLevel.STOP_BUS)
    public void stopBus() {
        eventBus.stopQueue();
    }

    @Override
    public PersistentBus getBus() {
        return eventBus;
    }
}
