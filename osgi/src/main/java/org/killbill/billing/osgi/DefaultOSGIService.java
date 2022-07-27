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

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.killbill.billing.osgi.config.OSGIConfig;
import org.killbill.billing.platform.api.LifecycleHandlerType;
import org.killbill.billing.platform.api.OSGIService;
import org.killbill.bus.api.PersistentBus;
import org.killbill.bus.api.PersistentBus.EventBusException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public class DefaultOSGIService implements OSGIService {


    private static final Logger logger = LoggerFactory.getLogger(DefaultOSGIService.class);

    private final OSGIConfig osgiConfig;
    private final KillbillActivator killbillActivator;
    private final BundleRegistry bundleRegistry;
    private final List<BundleWithConfig> installedBundles;
    private final PersistentBus externalBus;
    private final OSGIListener osgiListener;

    private Framework framework;

    @Inject
    public DefaultOSGIService(final OSGIConfig osgiConfig, final BundleRegistry bundleRegistry,
                              final KillbillActivator killbillActivator, @Named("externalBus") final PersistentBus externalBus,
                              final OSGIListener osgiListener) {
        this.osgiConfig = osgiConfig;
        this.killbillActivator = killbillActivator;
        this.bundleRegistry = bundleRegistry;
        this.externalBus = externalBus;
        this.osgiListener = osgiListener;
        this.installedBundles = new LinkedList<BundleWithConfig>();
        this.framework = null;
    }

    @Override
    public String getName() {
        return KILLBILL_SERVICES.OSGI_SERVICE.getServiceName();
    }

    @Override
    public int getRegistrationOrdering() {
        return KILLBILL_SERVICES.OSGI_SERVICE.getRegistrationOrdering();
    }

    @LifecycleHandlerType(LifecycleHandlerType.LifecycleLevel.INIT_PLUGIN)
    public void initialize() {
        try {
            // We start by deleting existing osi cache; we might optimize later keeping the cache
            pruneOSGICache();

            // Create the system bundle for killbill and start the framework
            this.framework = createAndInitFramework();
            framework.start();
            bundleRegistry.installBundles(framework);

            externalBus.register(osgiListener);
        } catch (final BundleException e) {
            logger.error("Failed to initialize Killbill OSGIService", e);
        } catch (final EventBusException e) {
            logger.error("Failed to initialize Killbill OSGIService", e);
        }
    }

    @LifecycleHandlerType(LifecycleHandlerType.LifecycleLevel.START_PLUGIN)
    public void start() {
        // This will call the start() method for the bundles
        bundleRegistry.startBundles();
        // Tell the plugins all bundles have started
        killbillActivator.sendEvent("org/killbill/billing/osgi/lifecycle/STARTED", new HashMap<String, String>());
    }

    @LifecycleHandlerType(LifecycleHandlerType.LifecycleLevel.STOP_PLUGIN)
    public void stop() {
        try {
            externalBus.unregister(osgiListener);

            framework.stop();
            framework.waitForStop(0);

            installedBundles.clear();

            // This will call the stop() method for the bundles
            bundleRegistry.stopBundles();
            // Tell the plugins all bundles have stopped
            killbillActivator.sendEvent("org/killbill/billing/osgi/lifecycle/STOPPED", new HashMap<String, String>());
        } catch (final BundleException e) {
            logger.error("Failed to Stop Killbill OSGIService " + e.getMessage());
        } catch (final InterruptedException e) {
            logger.error("Failed to Stop Killbill OSGIService " + e.getMessage());
        } catch (final EventBusException e) {
            logger.error("Failed to Stop Killbill OSGIService " + e.getMessage());
        }
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<BundleWithConfig> getInstalledBundles() {
        return installedBundles;
    }

    private Framework createAndInitFramework() throws BundleException {
        final StringBuilder systemExtraPackages = new StringBuilder(osgiConfig.getSystemBundleExportPackagesApi());
        if (!osgiConfig.getSystemBundleExportPackagesJava().isEmpty()) {
            systemExtraPackages
                    .append(",")
                    .append(osgiConfig.getSystemBundleExportPackagesJava());
        }
        if (!osgiConfig.getSystemBundleExportPackagesExtra().isEmpty()) {
            systemExtraPackages
                    .append(",")
                    .append(osgiConfig.getSystemBundleExportPackagesExtra());
        }

        final Map<String, String> config = new HashMap<String, String>();
        config.put("org.osgi.framework.system.packages.extra", systemExtraPackages.toString());
        config.put("felix.cache.rootdir", osgiConfig.getOSGIBundleRootDir());
        config.put("org.osgi.framework.storage", osgiConfig.getOSGIBundleCacheName());
        // Use the ext class loader as parent so that bundles can load java.sql.* on JDK 11
        config.put("org.osgi.framework.bundle.parent", "ext");
        return createAndInitFelixFrameworkWithSystemBundle(config);
    }

    private Framework createAndInitFelixFrameworkWithSystemBundle(final Map<String, String> config) throws BundleException {
        // From standard properties add Felix specific property to add a System bundle activator
        final Map<Object, Object> felixConfig = new HashMap<Object, Object>();
        felixConfig.putAll(config);

        // Install default bundles in the Framework: Killbill bundle only for now
        // Note! Think twice before adding a bundle here as it will run inside the System bundle. This means the bundle
        // callcontext that the bundle will see is the System bundle one, which will break e.g. resources lookup
        felixConfig.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP,
                        List.<BundleActivator>of(killbillActivator));

        final Framework felix = new Felix(felixConfig);
        felix.init();
        return felix;
    }

    private void pruneOSGICache() {
        final String path = osgiConfig.getOSGIBundleRootDir();
        deleteUnderDirectory(new File(path));
    }

    private static void deleteUnderDirectory(final File path) {
        deleteDirectory(path, false);
    }

    private static void deleteDirectory(final File path, final boolean deleteParent) {
        if (path == null) {
            return;
        }

        if (path.exists()) {
            final File[] files = path.listFiles();
            if (files != null) {
                for (final File f : files) {
                    if (f.isDirectory()) {
                        deleteDirectory(f, true);
                    } else if (!f.delete()) {
                        logger.warn("Unable to delete {}", f.getAbsolutePath());
                    }
                }
            }

            if (deleteParent) {
                if (!path.delete()) {
                    logger.warn("Unable to delete {}", path.getAbsolutePath());
                } else {
                    logger.info("Deleted recursively {}", path.getAbsolutePath());
                }
            }
        }
    }
}
