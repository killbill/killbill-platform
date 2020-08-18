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

package org.killbill.billing.service.registry;

import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.providers.DefaultEurekaClientConfigProvider;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.glue.KillBillPlatformModuleBase;
import org.killbill.billing.server.healthchecks.ServiceRegistry;

public class EurekaModule extends KillBillPlatformModuleBase {

    public EurekaModule(final KillbillConfigSource configSource) {
        super(configSource);
    }

    @Override
    protected void configure() {
        // need to eagerly initialize
        bind(ApplicationInfoManager.class).asEagerSingleton();

        bind(EurekaInstanceConfig.class).toProvider(KillbillEurekaInstanceConfigProvider.class).asEagerSingleton();
        bind(EurekaClientConfig.class).toProvider(DefaultEurekaClientConfigProvider.class).asEagerSingleton();

        // this is the self instanceInfo used for registration purposes
        bind(InstanceInfo.class).toProvider(KillbillEurekaInstanceInfoProvider.class).asEagerSingleton();

        bind(EurekaClient.class).to(DiscoveryClient.class).asEagerSingleton();
        bind(AbstractDiscoveryClientOptionalArgs.class).to(EurekaClientOptionalArgs.class).asEagerSingleton();

        Multibinder<ServiceRegistry> serviceRegistryBinder = Multibinder.newSetBinder(binder(), ServiceRegistry.class);
        serviceRegistryBinder.addBinding().to(EurekaServiceRegistry.class);
    }

    @Provides
    KillbillConfigSource killbillConfigSource() {
        return configSource;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
