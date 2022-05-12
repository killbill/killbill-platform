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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.killbill.billing.osgi.api.KillbillNodesApiHolder;
import org.killbill.billing.osgi.api.config.PluginConfig;
import org.killbill.billing.osgi.api.config.PluginConfigServiceApi;
import org.killbill.billing.osgi.api.config.PluginJavaConfig;
import org.killbill.billing.osgi.api.config.PluginLanguage;
import org.killbill.billing.osgi.api.config.PluginRubyConfig;
import org.killbill.billing.osgi.pluginconf.DefaultPluginConfigServiceApi;
import org.killbill.billing.osgi.pluginconf.PluginConfigException;
import org.killbill.billing.osgi.pluginconf.PluginFinder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO Pierre Should we leverage org.apache.felix.fileinstall.internal.FileInstall?
public class FileInstall {

    private static final Logger logger = LoggerFactory.getLogger(FileInstall.class);

    private final PureOSGIBundleFinder osgiBundleFinder;
    private final PluginFinder pluginFinder;
    private final PluginConfigServiceApi pluginConfigServiceApi;

    @Inject
    public FileInstall(final PureOSGIBundleFinder osgiBundleFinder, final PluginFinder pluginFinder, final KillbillNodesApiHolder nodesApiHolder, final PluginConfigServiceApi pluginConfigServiceApi) {
        this.osgiBundleFinder = osgiBundleFinder;
        this.pluginFinder = pluginFinder;
        this.pluginConfigServiceApi = pluginConfigServiceApi;
    }

    public List<BundleWithConfig> installBundles(final Framework framework) {

        final List<BundleWithConfig> installedBundles = new LinkedList<BundleWithConfig>();
        try {

            final BundleContext context = framework.getBundleContext();

            // Install all bundles and create service mapping
            installAllOSGIBundles(context, installedBundles);
            installAllJavaPluginBundles(context, installedBundles);
        } catch (final PluginConfigException e) {
            logger.error("Error while parsing plugin configurations", e);
        } catch (final BundleException e) {
            logger.error("Error while parsing plugin configurations", e);
        } catch (final IOException e) {
            logger.error("Error while parsing plugin configurations", e);
        }
        return installedBundles;
    }

    public BundleWithConfig installNewBundle(final String pluginName, @Nullable final String version, final Framework framework) {
        try {
            // Handle pure OSGI bundle case
            final String osgiBundlePath = osgiBundleFinder.getOSGIPath(pluginName);
            if (osgiBundlePath != null) {
                final Bundle bundle = installOSGIBundle(framework.getBundleContext(), osgiBundlePath);
                return new BundleWithConfig(bundle, null);
            }

            // Kill Bill plugins
            final List<PluginConfig> configs = pluginFinder.getVersionsForPlugin(pluginName, version);
            if (configs.isEmpty() || (version != null && configs.size() != 1)) {
                throw new PluginConfigException("Cannot install plugin " + pluginName + ", version = " + version);
            }

            final Bundle bundle = installBundle(configs.get(0), framework.getBundleContext(), configs.get(0).getPluginLanguage());
            return new BundleWithConfig(bundle, configs.get(0));
        } catch (final PluginConfigException e) {
            logger.error("Error while installing plugin " + pluginName, e);
        } catch (final BundleException e) {
            logger.error("Error while installing plugin" + pluginName, e);
        } catch (final IOException e) {
            logger.error("Error while installing plugin " + pluginName, e);
        }
        return null;
    }


    private void installAllOSGIBundles(final BundleContext context, final List<BundleWithConfig> installedBundles) throws PluginConfigException, BundleException {
        final List<String> bundleJarPaths = osgiBundleFinder.getLatestBundles();
        for (final String cur : bundleJarPaths) {
            try {
                final Bundle bundle = installOSGIBundle(context, cur);
                installedBundles.add(new BundleWithConfig(bundle, null));
            } catch (final BundleException e) {
                logger.error("Error while installing bundle {}, ignoring", cur, e);
            }
        }
    }

    private Bundle installOSGIBundle(final BundleContext context, final String path) throws BundleException {

        logger.info("Installing Java OSGI bundle from {}", path);

        final Bundle bundle = context.installBundle("file:" + path);
        osgiBundleFinder.recordMappingPluginNameToPath(bundle.getSymbolicName(), path);
        return bundle;
    }

    private void installAllJavaPluginBundles(final BundleContext context, final List<BundleWithConfig> installedBundles) throws PluginConfigException, BundleException, IOException {
        final List<PluginJavaConfig> pluginJavaConfigs = pluginFinder.getLatestJavaPlugins();
        for (final PluginJavaConfig cur : pluginJavaConfigs) {
            final Bundle bundle = installBundle(cur, context, PluginLanguage.JAVA);
            installedBundles.add(new BundleWithConfig(bundle, cur));
        }
    }

    private Bundle installBundle(final PluginConfig config, final BundleContext context, final PluginLanguage pluginLanguage) throws BundleException {

        Bundle bundle;
        switch (pluginLanguage) {
            case JAVA:
                final PluginJavaConfig javaConfig = (PluginJavaConfig) config;
                final String location = "file:" + javaConfig.getBundleJarPath();
                bundle = context.getBundle(location);
                if (bundle == null) {
                    logger.info("Installing Java bundle for plugin {} from {}", javaConfig.getPluginName(), javaConfig.getBundleJarPath());
                    bundle = context.installBundle(location);
                    ((DefaultPluginConfigServiceApi) pluginConfigServiceApi).registerBundle(bundle.getBundleId(), javaConfig);
                }
                break;
            default:
                throw new IllegalStateException("Unknown pluginLanguage " + pluginLanguage);
        }
        return bundle;
    }

    public boolean startBundle(final Bundle bundle) {
        if (bundle.getState() == Bundle.UNINSTALLED) {
            logger.info("Skipping uninstalled bundle {}", bundle.getLocation());
        } else if (isFragment(bundle)) {
            // Fragments can never be started.
            logger.info("Skipping fragment bundle {}", bundle.getLocation());
        } else {
            logger.info("Starting bundle {}", bundle.getLocation());
            try {
                bundle.start();
                return true;
            } catch (final BundleException e) {
                logger.warn("Unable to start bundle", e);
            }
        }
        return false;
    }

    /**
     * Check if a bundle is a fragment.
     *
     * @param bundle bundle to check
     * @return true iff the bundle is a fragment
     */
    private boolean isFragment(final Bundle bundle) {
        // Necessary cast on jdk7
        final BundleRevision bundleRevision = bundle.adapt(BundleRevision.class);
        return bundleRevision != null && (bundleRevision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
    }

}
