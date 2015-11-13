/*
 * Copyright 2015 Groupon, Inc
 * Copyright 2015 The Billing Project, LLC
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

package org.killbill.billing.osgi.api;

import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.killbill.billing.osgi.BundleRegistry;
import org.killbill.billing.osgi.BundleRegistry.BundleWithMetadata;
import org.osgi.framework.Bundle;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class DefaultPluginsInfoApi implements PluginsInfoApi {

    private final BundleRegistry bundleRegistry;

    @Inject
    public DefaultPluginsInfoApi(final BundleRegistry bundleRegistry) {
        this.bundleRegistry = bundleRegistry;
    }

    @Override
    public Iterable<PluginInfo> getPluginsInfo() {
        return Iterables.transform(Iterables.filter(bundleRegistry.getBundles(), new Predicate<BundleWithMetadata>() {
            @Override
            public boolean apply(final BundleWithMetadata input) {
                return input.getBundle().getSymbolicName() != null;
            }
        }), new Function<BundleWithMetadata, PluginInfo>() {
            @Nullable
            @Override
            public PluginInfo apply(final BundleWithMetadata input) {
                return new DefaultPluginInfo(input.getBundle().getSymbolicName(), input.getPluginName(), input.getVersion(), input.getBundle().getState() == Bundle.ACTIVE, input.getServiceNames());
            }
        });
    }

    public static final class DefaultPluginInfo implements PluginInfo {

        private final String pluginName;
        private final String pluginSymbolicName;
        private final String version;
        private final Set<PluginServiceInfo> services;
        private final boolean running;

        public DefaultPluginInfo(final String pluginSymbolicName, final String pluginName, final String version, final boolean running, final Set<PluginServiceInfo> services) {
            this.pluginSymbolicName = pluginSymbolicName;
            this.pluginName = pluginName;
            this.version = version;
            this.running = running;
            this.services = services;
        }

        @Override
        public String getBundleSymbolicName() {
            return pluginSymbolicName;
        }

        @Override
        public String getPluginName() {
            return pluginName;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public Set<PluginServiceInfo> getServices() {
            return services;
        }

        @Override
        public boolean isRunning() {
            return running;
        }
    }

    public static class DefaultPluginServiceInfo implements PluginServiceInfo {

        private final String serviceTypeName;
        private final String registrationName;

        public DefaultPluginServiceInfo(final String serviceTypeName, final String registrationName) {
            this.serviceTypeName = serviceTypeName;
            this.registrationName = registrationName;
        }

        @Override
        public String getServiceTypeName() {
            return serviceTypeName;
        }

        @Override
        public String getRegistrationName() {
            return registrationName;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DefaultPluginServiceInfo)) {
                return false;
            }

            final DefaultPluginServiceInfo that = (DefaultPluginServiceInfo) o;

            if (serviceTypeName != null ? !serviceTypeName.equals(that.serviceTypeName) : that.serviceTypeName != null) {
                return false;
            }
            return !(registrationName != null ? !registrationName.equals(that.registrationName) : that.registrationName != null);

        }

        @Override
        public int hashCode() {
            int result = serviceTypeName != null ? serviceTypeName.hashCode() : 0;
            result = 31 * result + (registrationName != null ? registrationName.hashCode() : 0);
            return result;
        }
    }
}
