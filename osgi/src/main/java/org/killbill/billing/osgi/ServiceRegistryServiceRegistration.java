/*
 * Copyright 2020-2022 Equinix, Inc
 * Copyright 2014-2022 The Billing Project, LLC
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

package org.killbill.billing.osgi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.killbill.billing.osgi.api.OSGIServiceDescriptor;
import org.killbill.billing.osgi.api.OSGIServiceRegistration;
import org.killbill.billing.osgi.api.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ServiceRegistryServiceRegistration implements OSGIServiceRegistration<ServiceRegistry> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryServiceRegistration.class);

    private final Map<String, ServiceRegistry> pluginRegistrations = new HashMap<String, ServiceRegistry>();

    @Override
    public void registerService(final OSGIServiceDescriptor desc, final ServiceRegistry service) {
        logger.info("Registering ServiceRegistry {}", desc.getRegistrationName());
        pluginRegistrations.put(desc.getRegistrationName(), service);
    }

    @Override
    public void unregisterService(final String serviceName) {
        logger.info("Unregistering ServiceRegistry {}", serviceName);
        pluginRegistrations.remove(serviceName);
    }

    @Override
    public ServiceRegistry getServiceForName(final String serviceName) {
        return pluginRegistrations.get(serviceName);
    }

    @Override
    public Set<String> getAllServices() {
        return pluginRegistrations.keySet();
    }

    @Override
    public Class<ServiceRegistry> getServiceType() {
        return ServiceRegistry.class;
    }
}
