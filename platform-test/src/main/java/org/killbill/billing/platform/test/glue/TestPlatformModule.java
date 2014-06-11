/*
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

package org.killbill.billing.platform.test.glue;

import java.util.Set;

import javax.annotation.Nullable;

import org.killbill.billing.lifecycle.DefaultLifecycle;
import org.killbill.billing.lifecycle.api.Lifecycle;
import org.killbill.billing.lifecycle.bus.ExternalPersistentBusConfig;
import org.killbill.billing.lifecycle.glue.BusModule;
import org.killbill.billing.lifecycle.glue.PersistentBusProvider;
import org.killbill.billing.osgi.api.OSGIConfigProperties;
import org.killbill.billing.osgi.glue.DefaultOSGIModule;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.api.KillbillService;
import org.killbill.billing.platform.glue.KillBillModule;
import org.killbill.billing.platform.glue.MetricsModule;
import org.killbill.billing.platform.glue.NotificationQueueModule;
import org.killbill.bus.api.PersistentBus;
import org.killbill.bus.api.PersistentBusConfig;

import com.google.inject.Key;
import com.google.inject.name.Names;

public abstract class TestPlatformModule extends KillBillModule {

    private static final String EXTERNAL_BUS = "externalBus";

    private final boolean withOSGI;
    private final OSGIConfigProperties osgiConfigProperties;
    private final Set<? extends KillbillService> services;

    protected TestPlatformModule(final KillbillConfigSource configSource, final boolean withOSGI, @Nullable final OSGIConfigProperties osgiConfigProperties, @Nullable final Set<? extends KillbillService> services) {
        super(configSource);
        this.withOSGI = withOSGI;
        this.osgiConfigProperties = osgiConfigProperties;
        this.services = services;
    }

    @Override
    protected void configure() {
        configureLifecycle();

        configureNotificationQ();

        configureBus();
        // For the bus
        install(new MetricsModule(configSource));

        if (withOSGI) {
            configureOSGI();
        }
    }

    protected void configureLifecycle() {
        if (services != null) {
            bind(Lifecycle.class).toInstance(new DefaultLifecycle(services));
        } else {
            bind(Lifecycle.class).to(DefaultLifecycle.class).asEagerSingleton();
        }
    }

    protected void configureBus() {
        install(new BusModule(BusModule.BusType.PERSISTENT, configSource));
    }

    protected void configureNotificationQ() {
        install(new NotificationQueueModule(configSource));
    }

    protected void configureOSGI() {
        final PersistentBusConfig extBusConfig = new ExternalPersistentBusConfig(skifeConfigSource);
        install(new DefaultOSGIModule(configSource, osgiConfigProperties));

        bind(PersistentBusProvider.class).annotatedWith(Names.named(EXTERNAL_BUS)).toInstance(new PersistentBusProvider(extBusConfig));
        bind(PersistentBus.class).annotatedWith(Names.named(EXTERNAL_BUS)).toProvider(Key.get(PersistentBusProvider.class, Names.named(EXTERNAL_BUS))).asEagerSingleton();
    }
}
