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

import javax.inject.Provider;

import com.google.inject.Inject;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.EurekaNamespace;

public class KillbillEurekaInstanceConfigProvider implements Provider<EurekaInstanceConfig> {
    @Inject(optional = true)
    @EurekaNamespace
    private String namespace;

    private KillbillEurekaInstanceConfig config;

    @Override
    public synchronized KillbillEurekaInstanceConfig get() {
        if (config == null) {
            if (namespace == null) {
                config = new KillbillEurekaInstanceConfig();
            } else {
                config = new KillbillEurekaInstanceConfig(namespace);
            }

            // TODO: Remove this when DiscoveryManager is finally no longer used
            DiscoveryManager.getInstance().setEurekaInstanceConfig(config);
        }
        return config;
    }
}