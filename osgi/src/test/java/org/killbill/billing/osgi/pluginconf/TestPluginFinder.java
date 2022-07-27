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

package org.killbill.billing.osgi.pluginconf;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import org.killbill.billing.osgi.api.config.PluginJavaConfig;
import org.killbill.billing.osgi.config.OSGIConfig;
import org.killbill.commons.util.io.Files;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.testng.Assert.assertEquals;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
public class TestPluginFinder {

    public static final String DEFAULT_PROPERTY_NAME = "killbill.properties";

    private PluginFinder pluginFinder;
    private File rootInstallationDir;
    private File platform;
    private File plugins;
    private File pluginsJava;
    private File pluginsRuby;

    @BeforeMethod(groups = "fast")
    public void beforeMethod() {
        rootInstallationDir = Files.createTempDirectory();

        final OSGIConfig osgiConfig = createOSGIConfig();
        pluginFinder = new PluginFinder(osgiConfig);

        platform = new File(rootInstallationDir, "platform");
        platform.mkdir();

        plugins = new File(rootInstallationDir, "plugins");
        plugins.mkdir();

        pluginsJava = new File(plugins, "java");
        pluginsJava.mkdir();


        pluginsRuby = new File(plugins, "ruby");
        pluginsRuby.mkdir();
    }


    private void createSymlinkPriorJava7(final File currentDirectory, final String currentFileName, final String linkFileName) throws IOException, InterruptedException {
        final Process process = Runtime.getRuntime().exec( new String[] { "ln", "-s", currentFileName, linkFileName }, new String[] {}, currentDirectory );
        final int error = process.waitFor();
        assertEquals(error, 0);
        process.destroy();
    }

    @Test(groups = "fast")
    public void testMultipleJavaVersions() throws IOException, InterruptedException, PluginConfigException {

        final File plugin = createNewJavaPlugin("FOO", new String[]{"1.2", "0.5", "1.0", "2.1", "0.8"}, null);

        final List<PluginJavaConfig> javaConfigs = pluginFinder.getLatestJavaPlugins();
        assertEquals(javaConfigs.size(), 1);

        final PluginJavaConfig javaConfig = javaConfigs.get(0);
        assertEquals(javaConfig.getPluginName(), "FOO");
        assertEquals(javaConfig.getVersion(), "2.1");
        assertEquals(javaConfig.getBundleJarPath(), plugin.getAbsolutePath() + "/2.1" + "/FOO.jar");
    }


    @Test(groups = "fast")
    public void testMultipleJavaVersionsWithDefault() throws IOException, InterruptedException, PluginConfigException {

        final File plugin = createNewJavaPlugin("BAR", new String[]{"1.2", "0.5", "1.0", "2.1", "0.8"}, "0.5");

        final List<PluginJavaConfig> javaConfigs = pluginFinder.getLatestJavaPlugins();
        assertEquals(javaConfigs.size(), 1);

        final PluginJavaConfig javaConfig = javaConfigs.get(0);
        assertEquals(javaConfig.getPluginName(), "BAR");
        assertEquals(javaConfig.getVersion(), "0.5");
        assertEquals(javaConfig.getBundleJarPath(), plugin.getAbsolutePath() + "/0.5" + "/BAR.jar");
    }

    @Test(groups = "fast")
    public void testJavaPluginWithDisabledHighest() throws IOException, InterruptedException, PluginConfigException {

        final File plugin = createNewJavaPlugin("ZOO", new String[]{"1.2", "0.5", "1.0", "2.1", "0.8"}, null);
        // In that case the code will default to the second highest known version
        addDisabledFile(plugin, "2.1");

        final List<PluginJavaConfig> javaConfigs = pluginFinder.getLatestJavaPlugins();
        assertEquals(javaConfigs.size(), 1);

        final PluginJavaConfig javaConfig = javaConfigs.get(0);
        assertEquals(javaConfig.getPluginName(), "ZOO");
        assertEquals(javaConfig.getVersion(), "1.2");
        assertEquals(javaConfig.getBundleJarPath(), plugin.getAbsolutePath() + "/1.2" + "/ZOO.jar");
    }


    @Test(groups = "fast")
    public void testJavaPluginWithAllDisabled() throws IOException, InterruptedException, PluginConfigException {

        final File plugin = createNewJavaPlugin("LOL", new String[]{"1.2", "0.5", "1.0", "2.1", "0.8"}, null);
        addDisabledFile(plugin, "0.5");
        addDisabledFile(plugin, "0.8");
        addDisabledFile(plugin, "1.0");
        addDisabledFile(plugin, "1.2");
        addDisabledFile(plugin, "2.1");

        final List<PluginJavaConfig> javaConfigs = pluginFinder.getLatestJavaPlugins();
        assertEquals(javaConfigs.size(), 0);
    }


    @Test(groups = "fast")
    public void testJavaPluginWithDisabledDefault() throws IOException, InterruptedException, PluginConfigException {

        final File plugin = createNewJavaPlugin("YEAH", new String[]{"1.2", "0.5", "1.0", "2.1", "0.8"}, "1.0");

        // In that case the code will default to the highest known version
        addDisabledFile(plugin, "1.0");

        final List<PluginJavaConfig> javaConfigs = pluginFinder.getLatestJavaPlugins();
        assertEquals(javaConfigs.size(), 1);

        final PluginJavaConfig javaConfig = javaConfigs.get(0);
        assertEquals(javaConfig.getPluginName(), "YEAH");
        assertEquals(javaConfig.getVersion(), "2.1");
        assertEquals(javaConfig.getBundleJarPath(), plugin.getAbsolutePath() + "/2.1" + "/YEAH.jar");

    }

    private void addDisabledFile(final File plugin, final String version) throws IOException {
        final File versionFile = new File(plugin, version);

        final File tmpFile = new File(versionFile, PluginFinder.TMP_DIR_NAME);
        tmpFile.mkdir();

        final File disabledFile = new File(tmpFile, PluginFinder.DISABLED_FILE_NAME);
        disabledFile.createNewFile();
    }

    private File createNewJavaPlugin(final String pluginName, final String[] pluginVersions, @Nullable final String defaultVersion) throws IOException, InterruptedException {
        final File newPlugin = new File(pluginsJava, pluginName);
        newPlugin.mkdir();

        for (final String cur : pluginVersions) {
            final File version = new File(newPlugin, cur);
            version.mkdir();

            final File jar = new File(version, pluginName + ".jar");
            jar.createNewFile();

            if (defaultVersion != null && defaultVersion.equals(cur)) {
                createSymlinkPriorJava7(newPlugin, cur, PluginFinder.SELECTED_VERSION_LINK_NAME);
            }
        }
        return newPlugin;
    }

    private OSGIConfig createOSGIConfig() {
        return new OSGIConfig() {
            @Override
            public String getOSGIKillbillPropertyName() {
                return DEFAULT_PROPERTY_NAME;
            }
            @Override
            public String getOSGIBundleRootDir() {
                return null;
            }
            @Override
            public String getOSGIBundleCacheName() {
                return null;
            }
            @Override
            public String getRootInstallationDir() {
                return rootInstallationDir.getAbsolutePath();
            }
            @Override
            public String getSystemBundleExportPackagesApi() {
                return null;
            }
            @Override
            public String getSystemBundleExportPackagesJava() {
                return null;
            }
            @Override
            public String getSystemBundleExportPackagesExtra() {
                return null;
            }
        };
    }

}
