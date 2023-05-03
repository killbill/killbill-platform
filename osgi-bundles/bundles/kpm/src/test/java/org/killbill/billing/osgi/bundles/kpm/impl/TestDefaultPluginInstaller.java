/*
 * Copyright 2020-2023 Equinix, Inc
 * Copyright 2014-2023 The Billing Project, LLC
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

package org.killbill.billing.osgi.bundles.kpm.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.killbill.billing.osgi.bundles.kpm.KpmProperties;
import org.killbill.billing.osgi.bundles.kpm.PluginFileService;
import org.killbill.billing.osgi.bundles.kpm.TestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestDefaultPluginInstaller {

    private static final String SET_DEFAULT_NAME = DefaultPluginFileService.DEFAULT_SYMLINK_NAME;
    private final Path DOWNLOADED_FILE = TestUtils.getTestPath("plugins", "uri-installer-test", "temp", "downloaded-file-jar.txt");

    private Path bundleInstallDir;

    private DefaultPluginInstaller pluginInstaller;

    @BeforeMethod(groups = "fast")
    public void beforeMethod() throws IOException {
        bundleInstallDir = Files.createTempDirectory("test-kpm-plugin-installer");
        final KpmProperties kpmProperties = TestUtils.getKpmProperties(bundleInstallDir.toString());
        final PluginFileService pluginFileService = new DefaultPluginFileService(kpmProperties);
        pluginInstaller = new DefaultPluginInstaller(pluginFileService);
    }

    @AfterMethod(groups = "fast")
    public void afterMethod() {
        FilesUtils.deleteRecursively(bundleInstallDir);
    }

    @Test(groups = "fast")
    public void testInstall() {
        // Make sure resources test files available
        Assert.assertTrue(Files.exists(DOWNLOADED_FILE));

        // Will be installed to OS temp directory named 'test-kpm-plugin-installer' See #beforeMethod() above
        pluginInstaller.install(DOWNLOADED_FILE, "superjar", "1.0.0-SNAPSHOT");

        final Path installedPluginDir = Path.of(bundleInstallDir.toString(), // killbill bundle path
                                                "plugins", "java", // "plugin/java" created by pluginFileService.createPluginDirectory()
                                                "superjar-plugin", // "superjar" is pluginKey (see pluginInstaller.install() params). We always add "-plugin"
                                                "1.0.0"); // "snapshot should get removed

        final Path installedPluginFile = installedPluginDir.resolve("superjar-plugin-1.0.0.jar");
        // symlink created by pluginInstaller.install().
        final Path symlink = installedPluginDir.getParent().resolve(DefaultPluginFileService.DEFAULT_SYMLINK_NAME);

        Assert.assertTrue(Files.exists(installedPluginDir));
        Assert.assertTrue(Files.exists(installedPluginFile));
        Assert.assertTrue(Files.isRegularFile(installedPluginFile));
        Assert.assertTrue(Files.exists(symlink));
        Assert.assertTrue(Files.isSymbolicLink(symlink));
    }

    @DataProvider(name = "testUninstallParams")
    Object[][] testUninstallParams() {
        return new Object[][] {
                { "1.0.0", "1.0.2" },
                { "1.0.1", "1.0.2" },
                { "1.0.2", "1.0.1" }
        };
    }

    @Test(groups = "fast", dataProvider = "testUninstallParams")
    public void testUninstall(final String uninstalledVersion, final String setDefaultVersionAfterUninstall) {
        beforeTestUninstall();

        // do uninstall
        pluginInstaller.uninstall("superjar", uninstalledVersion);

        final Path pluginKeyDir = Path.of(bundleInstallDir.toString(), "plugins", "java", "superjar-plugin");
        final Path uninstalledPluginDir = pluginKeyDir.resolve(uninstalledVersion);
        final Path uninstalledPluginJar = uninstalledPluginDir.resolve("superjar-plugin-" + uninstalledVersion + ".jar");
        final Path symlinkAfterUninstall = pluginKeyDir
                .resolve(SET_DEFAULT_NAME)
                .resolve("superjar-plugin-" + setDefaultVersionAfterUninstall + ".jar");

        Assert.assertFalse(Files.exists(pluginKeyDir.resolve(uninstalledVersion)));
        Assert.assertFalse(Files.exists(uninstalledPluginDir));
        Assert.assertFalse(Files.exists(uninstalledPluginJar));
        Assert.assertTrue(Files.exists(symlinkAfterUninstall));
    }

    private void beforeTestUninstall() {
        pluginInstaller.install(DOWNLOADED_FILE, "superjar", "1.0.0");
        pluginInstaller.install(DOWNLOADED_FILE, "superjar", "1.0.1");
        pluginInstaller.install(DOWNLOADED_FILE, "superjar", "1.0.2"); // SET_DEFAULT dir will be pointed to this

        // Make sure everything get installed correctly
        final Path pluginKeyDir = Path.of(bundleInstallDir.toString(), "plugins", "java", "superjar-plugin");
        Assert.assertTrue(Files.exists(pluginKeyDir.resolve("1.0.0")));
        Assert.assertTrue(Files.exists(pluginKeyDir.resolve("1.0.0").resolve("superjar-plugin-1.0.0.jar")));
        Assert.assertTrue(Files.exists(pluginKeyDir.resolve("1.0.1")));
        Assert.assertTrue(Files.exists(pluginKeyDir.resolve("1.0.1").resolve("superjar-plugin-1.0.1.jar")));
        Assert.assertTrue(Files.exists(pluginKeyDir.resolve("1.0.2")));
        Assert.assertTrue(Files.exists(pluginKeyDir.resolve("1.0.2").resolve("superjar-plugin-1.0.2.jar")));

        // symlink assertion. SET_DEFAULT/superjar-plugin-1.0.2.jar exists because this is the latest file that get installed.
        Assert.assertTrue(Files.exists(pluginKeyDir.resolve(SET_DEFAULT_NAME).resolve("superjar-plugin-1.0.2.jar")));
        Assert.assertFalse(Files.exists(pluginKeyDir.resolve(SET_DEFAULT_NAME).resolve("superjar-plugin-1.0.1.jar")));
    }
}

