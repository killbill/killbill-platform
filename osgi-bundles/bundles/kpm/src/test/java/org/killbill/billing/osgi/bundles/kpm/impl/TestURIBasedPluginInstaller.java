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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestURIBasedPluginInstaller {

    private final Path testPath = TestUtils.getTestPath("plugins", "uri-installer-test", "temp");

    private URIBasedPluginInstaller pluginInstaller;

    @BeforeMethod(groups = "fast")
    public void beforeMethod() throws IOException {
        final PluginFileService pluginFileService = new DefaultPluginFileService(new KpmProperties(TestUtils.getTestProperties()));
        final Path downloadedDir = Files.createDirectories(testPath);
        final Path downloadedFile = downloadedDir.resolve("downloaded-file-jar.txt");
        if (!Files.exists(downloadedFile)) {
            Files.createFile(downloadedFile);
        }
        pluginInstaller = new URIBasedPluginInstaller(pluginFileService, downloadedFile, "superjar", "1.0.0-SNAPSHOT");
    }

    @Test(groups = "fast")
    public void testInstall() {
        // Make sure all test files available
        Assert.assertTrue(Files.exists(testPath));
        Assert.assertTrue(Files.exists(testPath.resolve("downloaded-file-jar.txt")));

        pluginInstaller.install();

        final Path pluginDirectory = TestUtils.getTestPath("plugins", "java", // "plugin/java" created by pluginFileService.createPluginDirectory()
                                                           "superjar-plugin", // "superjar" is pluginKey (see above). We always add "-plugin"
                                                           "1.0.0"); // "snapshot should get removed
        final Path pluginFile = pluginDirectory.resolve("superjar-plugin-1.0.0.jar");
        final Path symlink = TestUtils.getTestPath("plugins", "java", "superjar-plugin", DefaultPluginFileService.DEFAULT_SYMLINK_NAME);

        Assert.assertTrue(Files.exists(pluginDirectory));
        Assert.assertTrue(Files.exists(pluginFile));
        Assert.assertTrue(Files.isRegularFile(pluginFile));
        Assert.assertTrue(Files.exists(symlink));
    }
}

