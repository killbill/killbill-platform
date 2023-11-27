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

package org.killbill.billing.osgi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.killbill.billing.osgi.api.DefaultPluginsInfoApi.DefaultPluginServiceInfo;
import org.killbill.billing.osgi.api.OSGIServiceDescriptor;
import org.killbill.billing.osgi.api.PluginServiceInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleRegistry {

    private static final Logger log = LoggerFactory.getLogger(BundleRegistry.class);

    private final FileInstall fileInstall;
    private final Map<String, BundleWithMetadata> registry;

    private Framework framework;

    // We keep track of those to maintain the ordering on start, but probably we don't need to
    private List<BundleWithConfig> bundleWithConfigs;

    @Inject
    public BundleRegistry(final FileInstall fileInstall) {
        this.fileInstall = fileInstall;
        this.registry = new HashMap<String, BundleWithMetadata>();
    }

    public void installBundles(final Framework framework) {
        // Keep a copy of the framework during initialization phase when we first install all bundles
        this.framework = framework;
        bundleWithConfigs = fileInstall.installBundles(framework);
        for (final BundleWithConfig bundleWithConfig : bundleWithConfigs) {
            registry.put(getPluginName(bundleWithConfig), new BundleWithMetadata(bundleWithConfig));
        }
    }

    public BundleWithMetadata installAndStartNewBundle(final String pluginName, @Nullable final String pluginVersion) throws BundleException {

        final BundleWithMetadata bundle = registry.get(pluginName);
        if (bundle != null) {
            // We don't try to be too smart, we let the user first stop existing bundle if needed
            throw new IllegalStateException(String.format("Plugin %s version %s cannot be started because the version %s already exists in the registry (state = %s)",
                                                          bundle.getPluginName(), pluginVersion, bundle.getVersion(), bundle.getBundle().getState()));
        }
        final BundleWithConfig bundleWithConfig = fileInstall.installNewBundle(pluginName, pluginVersion, framework);
        final BundleWithMetadata bundleWithMetadata = new BundleWithMetadata(bundleWithConfig);
        if (fileInstall.startBundle(bundleWithConfig.getBundle())) {
            registry.put(getPluginName(bundleWithConfig), bundleWithMetadata);
        }
        return bundleWithMetadata;
    }

    public BundleWithMetadata stopAndUninstallNewBundle(final String pluginName, @Nullable final String pluginVersion) throws BundleException {

        final BundleWithMetadata bundle = registry.get(pluginName);
        if (bundle != null && (pluginVersion == null || bundle.getVersion().equals(pluginVersion))) {
            stopAndUninstallBundle(bundle.getBundle(), pluginName);
        }
        return bundle;
    }

    private void stopAndUninstallBundle(final Bundle bundle, final String pluginName) throws BundleException {
        if (bundle.getState() == Bundle.ACTIVE) {
            bundle.stop();
        }
        // The spec says that uninstall should always succeed
        bundle.uninstall();
        registry.remove(pluginName);
    }

    public void startBundles(final Iterable<String> mandatoryPlugins) throws Exception {
        log.info("List of mandatory plugins: {}", mandatoryPlugins);
        final List<String> pluginsStarted = new LinkedList<>();
        for (final BundleWithConfig bundleWithConfig : bundleWithConfigs) {
            final boolean isBundleStarted = fileInstall.startBundle(bundleWithConfig.getBundle());

            final String pluginName = getPluginName(bundleWithConfig);
            if (isBundleStarted) {
                pluginsStarted.add(pluginName);
            } else {
                registry.remove(pluginName);
            }

        }

        if (mandatoryPlugins.iterator().hasNext()) {
            checkIfMandatoryPluginsAreStarted(pluginsStarted, mandatoryPlugins);
        }
        else {
            log.info("Mandatory plugins not specified, skipping mandatory plugins check");
        }

    }

    private void checkIfMandatoryPluginsAreStarted(final List<String> pluginsStarted, final Iterable<String> mandatoryPlugins) throws Exception {
        for (final String pluginName : mandatoryPlugins) {
            if (!pluginsStarted.contains(pluginName)) {
                log.warn("Mandatory plugin {} not started", pluginName);
                throw new Exception("Mandatory plugin " + pluginName + " not started");
            }
        }
        log.info("All mandatory plugins are started");
    }


    public void stopBundles() {
        for (final BundleWithConfig bundleWithConfig : bundleWithConfigs) {
            try {
                if (bundleWithConfig.getBundle() != null && bundleWithConfig.getConfig() != null) {
                    stopAndUninstallBundle(bundleWithConfig.getBundle(), bundleWithConfig.getConfig().getPluginName());
                }
            } catch (final BundleException e) {
                log.warn("Unable to stop bundle", e);
            }
        }
    }

    public Iterable<BundleWithMetadata> getPureOSGIBundles() {
        return registry.values().stream()
                .filter(input -> input != null && input.getConfig() == null)
                .collect(Collectors.toUnmodifiableList());
    }

    public BundleWithMetadata getBundle(final String pluginName) {
        return registry.get(pluginName);
    }

    public String getPluginName(final Bundle bundle) {
        for (final BundleWithMetadata cur : registry.values()) {
            if (bundle.getSymbolicName().equals(cur.getBundle().getSymbolicName())) {
                return getPluginName(cur);
            }
        }
        return bundle.getSymbolicName();
    }

    public void registerService(final OSGIServiceDescriptor desc, final String serviceName) {
        for (final BundleWithMetadata cur : registry.values()) {
            if (desc.getPluginSymbolicName().equals(cur.getBundle().getSymbolicName())) {
                cur.register(desc.getRegistrationName(), serviceName);
            }
        }
    }

    public void unregisterService(final OSGIServiceDescriptor desc, final String serviceName) {
        for (final BundleWithMetadata cur : registry.values()) {
            if (desc.getPluginSymbolicName().equals(cur.getBundle().getSymbolicName())) {
                cur.unregister(desc.getRegistrationName(), serviceName);
            }
        }
    }

    private static String getPluginName(final BundleWithConfig bundleWithConfig) {
        return bundleWithConfig.getConfig() != null && bundleWithConfig.getConfig().getPluginName() != null ? bundleWithConfig.getConfig().getPluginName() : bundleWithConfig.getBundle().getSymbolicName();
    }

    public static class BundleWithMetadata extends BundleWithConfig {

        private final Set<PluginServiceInfo> serviceNames;

        public BundleWithMetadata(final BundleWithConfig bundleWithConfig) {
            super(bundleWithConfig.getBundle(), bundleWithConfig.getConfig());
            serviceNames = new HashSet<>();
        }

        public String getPluginName() {
            return BundleRegistry.getPluginName(this);
        }

        public String getVersion() {
            return getConfig() != null ? getConfig().getVersion() : null;
        }

        public void register(final String registrationName, final String serviceTypeName) {
            serviceNames.add(new DefaultPluginServiceInfo(serviceTypeName, registrationName));
        }

        public void unregister(final String registrationName, final String serviceTypeName) {
            serviceNames.remove(new DefaultPluginServiceInfo(serviceTypeName, registrationName));
        }

        public Set<PluginServiceInfo> getServiceNames() {
            return new HashSet<>(serviceNames);
        }
    }

}
