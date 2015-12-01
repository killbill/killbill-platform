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

package org.killbill.billing.osgi.pluginconf;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import org.killbill.billing.osgi.config.OSGIConfig;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Files;

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
        rootInstallationDir = Files.createTempDir();
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

    private File createNewJavaPlugin(final String pluginName, final String[] pluginVersions, @Nullable final String defaultVersion) throws IOException {
        final File newPlugin = new File(pluginsJava, "pluginName");
        newPlugin.mkdir();

        for (final String cur : pluginVersions) {
            final File version = new File(newPlugin, cur);
            version.mkdir();

            final File jar = new File(version, pluginName + ".jar");
            jar.createNewFile();

            if (defaultVersion != null && defaultVersion.equals(cur)) {


                final File def = new File(newPlugin, PluginFinder.SELECTED_VERSION_LINK_NAME);
                def.
            }
        }


    }


    private void createSymlinkPriorJava7(final String currentFileName, final String linkFileName) throws IOException, InterruptedException {
        final Process process = Runtime.getRuntime().exec( new String[] { "ln", "-s", currentFileName, linkFileName } );

        process.
        final int error = process.waitFor();
        Assert.assertEquals(error, 0);
        process.destroy();
    }

    @Test(groups = "fast")
    public void testFoo() {



        //pluginFinder.
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
