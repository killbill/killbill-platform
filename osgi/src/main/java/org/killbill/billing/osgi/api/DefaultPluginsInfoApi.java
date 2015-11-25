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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.killbill.billing.osgi.BundleRegistry;
import org.killbill.billing.osgi.BundleRegistry.BundleWithMetadata;
import org.killbill.billing.osgi.api.config.PluginConfig;
import org.killbill.billing.osgi.api.config.PluginLanguage;
import org.killbill.billing.osgi.pluginconf.PluginConfigException;
import org.killbill.billing.osgi.pluginconf.PluginFinder;
import org.killbill.billing.util.nodes.KillbillNodesApi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;

public class DefaultPluginsInfoApi implements PluginsInfoApi {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPluginsInfoApi.class);

    private static final Ordering<PluginInfo> PLUGIN_INFO_ORDERING =  Ordering.natural().onResultOf(new Function<PluginInfo, String>() {
        @Override
        public String apply(final PluginInfo input) {
            return toPluginFullName(input.getPluginName(), input.getVersion());
        }

        private String toPluginFullName(final String pluginName, final String pluginVersion) {
            final StringBuilder tmp = new StringBuilder(pluginName);
            if (pluginVersion != null) {
                tmp.append(pluginVersion);
            }
            return tmp.toString();
        }
    });


    private final BundleRegistry bundleRegistry;
    private final KillbillNodesApi nodesApi;
    private final PluginFinder pluginFinder;

    @Inject
    public DefaultPluginsInfoApi(final BundleRegistry bundleRegistry, final PluginFinder pluginFinder, final KillbillNodesApi nodesApi) {
        this.bundleRegistry = bundleRegistry;
        this.pluginFinder = pluginFinder;
        this.nodesApi = nodesApi;
    }

    @Override
    public Iterable<PluginInfo> getPluginsInfo() {

        final List<PluginInfo> result = new ArrayList<PluginInfo>();
        for (final String pluginName : pluginFinder.getAllPlugins().keySet()) {

            final BundleWithMetadata installedBundleOrNull = bundleRegistry.getBundle(pluginName);

            final LinkedList<PluginConfig> pluginVersions = pluginFinder.getAllPlugins().get(pluginName);
            for (PluginConfig curVersion : pluginVersions) {
                final PluginInfo pluginInfo;
                if (installedBundleOrNull != null && curVersion.getVersion().equals(installedBundleOrNull.getVersion())) {
                    pluginInfo = new DefaultPluginInfo(installedBundleOrNull.getBundle().getSymbolicName(),
                                                       installedBundleOrNull.getPluginName(),
                                                       installedBundleOrNull.getVersion(),
                                                       toPluginState(installedBundleOrNull),
                                                       installedBundleOrNull.getServiceNames());
                } else {
                    pluginInfo = new DefaultPluginInfo(null, curVersion.getPluginName(), curVersion.getVersion(), toPluginState(null), ImmutableSet.<PluginServiceInfo>of());
                }
                result.add(pluginInfo);
            }
        }
        return PLUGIN_INFO_ORDERING.sortedCopy(result);
    }

    @Override
    public void notifyOfStateChanged(final PluginStateChange newState, final String pluginName, final String pluginVersion, final PluginLanguage pluginLanguage) {
        try {

            // Refresh our filesystem view so it shows up/disappears in the list of installed plugin
            pluginFinder.reloadPlugins();

            switch (newState) {
                case NEW_VERSION:
                    // Nothing special to do; we don't try to OSGI 'install' the plugin at this time, this will be done
                    // when we start it
                    break;

                case DISABLED:
                    // If plugin is in the bundleRegistry, we remove it (stopping it if required)
                    bundleRegistry.stopAndUninstallNewBundle(pluginName, pluginVersion);
                    break;

                default:
                    throw new IllegalStateException("Invalid PluginStateChange " + newState);
            }

            // Notify KillbillNodesService to update the node_infos table
            final PluginInfo pluginInfo = new DefaultPluginInfo(null, pluginName, pluginVersion, toPluginState(null), ImmutableSet.<PluginServiceInfo>of());
            nodesApi.notifyPluginChanged(pluginInfo);

        } catch (final PluginConfigException e) {
            logger.error("Failed to handle notifyOfStateChanged: ", e);
        } catch (final IOException e) {
            logger.error("Failed to handle notifyOfStateChanged: ", e);
        } catch (final BundleException e) {
            logger.error("Failed to handle notifyOfStateChanged: ", e);
        }
    }

    public static PluginState toPluginState(@Nullable final BundleWithMetadata bundle) {
        if (bundle == null) {
            return PluginState.INSTALLED;
        } else {
            return bundle.getBundle().getState() == Bundle.ACTIVE ? PluginState.RUNNING : PluginState.STOPPED;
        }
    }

    public static final class DefaultPluginInfo implements PluginInfo {

        private final String pluginName;
        private final String pluginSymbolicName;
        private final String version;
        private final Set<PluginServiceInfo> services;
        private final PluginState state;

        public DefaultPluginInfo(final String pluginSymbolicName, final String pluginName, final String version, final PluginState state, final Set<PluginServiceInfo> services) {
            this.pluginSymbolicName = pluginSymbolicName;
            this.pluginName = pluginName;
            this.version = version;
            this.state = state;
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
        public PluginState getPluginState() {
            return state;
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
