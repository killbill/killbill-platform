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

import org.killbill.billing.lifecycle.DefaultLifecycle;
import org.killbill.billing.lifecycle.api.Lifecycle;
import org.killbill.billing.lifecycle.config.LifecycleConfig;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.glue.KillBillPlatformModuleBase;
import org.skife.config.AugmentedConfigurationObjectFactory;

public class LifecycleModule extends KillBillPlatformModuleBase {

    public LifecycleModule(final KillbillConfigSource configSource) {
        super(configSource);
    }

    @Override
    protected void configure() {
        installLifecycle();
    }

    protected void configureConfig() {
        final LifecycleConfig lifecycleConfig = new AugmentedConfigurationObjectFactory(skifeConfigSource).build(LifecycleConfig.class);
        bind(LifecycleConfig.class).toInstance(lifecycleConfig);
    }

    protected void installLifecycle() {
        configureConfig();
        bind(Lifecycle.class).to(DefaultLifecycle.class).asEagerSingleton();
    }
}

