/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.annotation.Nullable;

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
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

// TODO Pierre Should we leverage org.apache.felix.fileinstall.internal.FileInstall?
public class FileInstall {

    private static final Logger logger = LoggerFactory.getLogger(FileInstall.class);

    private final PureOSGIBundleFinder osgiBundleFinder;
    private final PluginFinder pluginFinder;
    private final PluginConfigServiceApi pluginConfigServiceApi;
    private final AtomicInteger jrubyUniqueIndex;

    public FileInstall(final PureOSGIBundleFinder osgiBundleFinder, final PluginFinder pluginFinder, final PluginConfigServiceApi pluginConfigServiceApi) {
        this.osgiBundleFinder = osgiBundleFinder;
        this.pluginFinder = pluginFinder;
        this.pluginConfigServiceApi = pluginConfigServiceApi;
        this.jrubyUniqueIndex = new AtomicInteger(0);
    }

    public List<BundleWithConfig> installBundles(final Framework framework) {

        final List<BundleWithConfig> installedBundles = new LinkedList<BundleWithConfig>();
        try {

            final BundleContext context = framework.getBundleContext();

            final String jrubyBundlePath = findJrubyBundlePath();

            // Install all bundles and create service mapping
            installAllOSGIBundles(context, installedBundles, jrubyBundlePath);
            installAllJavaPluginBundles(context, installedBundles, jrubyBundlePath);
            installAllJRubyPluginBundles(context, installedBundles, jrubyBundlePath);

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
            final String jrubyBundlePath = findJrubyBundlePath();

            final Bundle bundle = installBundle(configs.get(0), framework.getBundleContext(), configs.get(0).getPluginLanguage(), jrubyBundlePath);
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


    private void installAllOSGIBundles(final BundleContext context, final List<BundleWithConfig> installedBundles, final String jrubyBundlePath) throws PluginConfigException, BundleException {
        final List<String> bundleJarPaths = osgiBundleFinder.getLatestBundles();
        for (final String cur : bundleJarPaths) {
            // Don't install the jruby.jar bundle
            if (jrubyBundlePath != null && jrubyBundlePath.equals(cur)) {
                continue;
            }

            final Bundle bundle = installOSGIBundle(context, cur);
            installedBundles.add(new BundleWithConfig(bundle, null));
        }
    }

    private Bundle installOSGIBundle(final BundleContext context, final String path) throws BundleException {

        logger.info("Installing Java OSGI bundle from {}", path);

        final Bundle bundle = context.installBundle("file:" + path);
        osgiBundleFinder.recordMappingPluginNameToPath(bundle.getSymbolicName(), path);
        return bundle;
    }

    private void installAllJavaPluginBundles(final BundleContext context, final List<BundleWithConfig> installedBundles, final String jrubyBundlePath) throws PluginConfigException, BundleException, IOException {
        final List<PluginJavaConfig> pluginJavaConfigs = pluginFinder.getLatestJavaPlugins();
        for (final PluginJavaConfig cur : pluginJavaConfigs) {
            final Bundle bundle = installBundle(cur, context, PluginLanguage.JAVA, jrubyBundlePath);
            installedBundles.add(new BundleWithConfig(bundle, cur));
        }
    }

    private void installAllJRubyPluginBundles(final BundleContext context, final List<BundleWithConfig> installedBundles, final String jrubyBundlePath) throws PluginConfigException, BundleException, IOException {
        if (jrubyBundlePath == null) {
            return;
        }

        final List<PluginRubyConfig> pluginRubyConfigs = pluginFinder.getLatestRubyPlugins();
        for (final PluginRubyConfig cur : pluginRubyConfigs) {
            final Bundle bundle = installBundle(cur, context, PluginLanguage.RUBY, jrubyBundlePath);
            installedBundles.add(new BundleWithConfig(bundle, cur));
        }
    }

    private Bundle installBundle(final PluginConfig config, final BundleContext context, final PluginLanguage pluginLanguage, final String jrubyBundlePath) throws BundleException {

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

            case RUBY:
                final PluginRubyConfig rubyConfig = (PluginRubyConfig) config;
                final String uniqueJrubyBundlePath = "jruby-" + rubyConfig.getPluginName();
                bundle = context.getBundle(uniqueJrubyBundlePath);
                if (bundle == null) {
                    logger.info("Installing JRuby bundle for plugin {} ", uniqueJrubyBundlePath);
                    InputStream tweakedInputStream = null;
                    try {
                        tweakedInputStream = tweakRubyManifestToBeUnique(jrubyBundlePath, jrubyUniqueIndex.incrementAndGet());
                        bundle = context.installBundle(uniqueJrubyBundlePath, tweakedInputStream);
                        ((DefaultPluginConfigServiceApi) pluginConfigServiceApi).registerBundle(bundle.getBundleId(), rubyConfig);
                    } catch (final IOException e) {
                        logger.warn("Failed to open file {}", jrubyBundlePath);
                    } finally {
                        if (tweakedInputStream != null) {
                            try {
                                tweakedInputStream.close();
                            } catch (final IOException ignore) {
                            }
                        }
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unknown pluginLanguage " + pluginLanguage);
        }
        return bundle;
    }

    private InputStream tweakRubyManifestToBeUnique(final String rubyJar, final int index) throws IOException {

        final Attributes.Name attrName = new Attributes.Name(Constants.BUNDLE_SYMBOLICNAME);
        final JarInputStream in = new JarInputStream(new FileInputStream(new File(rubyJar)));
        final Manifest manifest = in.getManifest();

        final Object currentValue = manifest.getMainAttributes().get(attrName);
        manifest.getMainAttributes().put(attrName, currentValue.toString() + "-" + index);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final JarOutputStream jarOut = new JarOutputStream(out, manifest);
        try {
            JarEntry e = in.getNextJarEntry();
            while (e != null) {
                if (!e.getName().equals(JarFile.MANIFEST_NAME)) {
                    jarOut.putNextEntry(e);
                    ByteStreams.copy(in, jarOut);
                }
                e = in.getNextJarEntry();
            }

        } finally {
            if (jarOut != null) {
                jarOut.close();
            }
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private String findJrubyBundlePath() {
        final String expectedPath = osgiBundleFinder.getPlatformOSGIBundlesRootDir() + "jruby.jar";
        if (new File(expectedPath).isFile()) {
            return expectedPath;
        } else {
            logger.warn("Unable to find the JRuby bundle at {}, ruby plugins won't be started!", expectedPath);
            return null;
        }
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
        final BundleRevision bundleRevision = (BundleRevision) bundle.adapt(BundleRevision.class);
        return bundleRevision != null && (bundleRevision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
    }

}
