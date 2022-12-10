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

import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Singleton;

import org.killbill.billing.osgi.api.OSGIServiceDescriptor;
import org.killbill.billing.osgi.api.OSGISingleServiceRegistration;
import org.killbill.commons.metrics.api.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MetricRegistryServiceRegistration implements OSGISingleServiceRegistration<MetricRegistry> {

    private static final Logger logger = LoggerFactory.getLogger(MetricRegistryServiceRegistration.class);

    private Entry<String, MetricRegistry> pluginRegistration;
    private final List<Runnable> listeners = new LinkedList<>();

    @Override
    public void registerService(final OSGIServiceDescriptor desc, final MetricRegistry service) {
        if (pluginRegistration != null) {
            logger.warn("MetricRegistry {} is already registered, ignoring {}", pluginRegistration.getKey(), desc.getRegistrationName());
        } else {
            logger.info("Registering MetricRegistry {}", desc.getRegistrationName());
            pluginRegistration = new SimpleEntry<>(desc.getRegistrationName(), service);
            onServiceChange();
        }
    }

    @Override
    public void unregisterService(final String serviceName) {
        if (pluginRegistration == null || !pluginRegistration.getKey().equals(serviceName)) {
            return;
        }

        pluginRegistration = null;
        onServiceChange();
    }

    @Override
    public MetricRegistry getService() {
        if (pluginRegistration == null) {
            return null;
        }
        return pluginRegistration.getValue();
    }

    @Override
    public Class<MetricRegistry> getServiceType() {
        return MetricRegistry.class;
    }

    @Override
    public void addRegistrationListener(final Runnable listener) {
        listeners.add(listener);
    }

    private void onServiceChange() {
        for (final Runnable runnable : listeners) {
            try {
                runnable.run();
            } catch (final RuntimeException e) {
                logger.warn("Ignoring Service Listener exception", e);
            }
        }
    }
}
