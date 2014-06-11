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

package org.killbill.billing.lifecycle.glue;

import org.killbill.billing.lifecycle.DefaultLifecycle;
import org.killbill.billing.lifecycle.api.Lifecycle;
import org.killbill.billing.lifecycle.bus.ExternalPersistentBusConfig;
import org.killbill.bus.api.PersistentBus;
import org.killbill.bus.api.PersistentBusConfig;
import org.skife.config.ConfigSource;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Names;

public class LifecycleModule extends AbstractModule {

    public static final String EXTERNAL_BUS = "externalBus";

    private final ConfigSource configSource;

    public LifecycleModule(final ConfigSource configSource) {
        this.configSource = configSource;
    }

    @Override
    protected void configure() {
        installLifecycle();
        installExternalBus();
    }

    protected void installLifecycle() {
        bind(Lifecycle.class).to(DefaultLifecycle.class).asEagerSingleton();
    }

    protected void installExternalBus() {
        final PersistentBusConfig extBusConfig = new ExternalPersistentBusConfig(configSource);

        bind(PersistentBusProvider.class).annotatedWith(Names.named(EXTERNAL_BUS)).toInstance(new PersistentBusProvider(extBusConfig));
        bind(PersistentBus.class).annotatedWith(Names.named(EXTERNAL_BUS)).toProvider(Key.get(PersistentBusProvider.class, Names.named(EXTERNAL_BUS))).asEagerSingleton();
    }
}

