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

import org.killbill.billing.osgi.bundles.kpm.TestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.testng.Assert;

public class TestDefaultPluginFileService {
    private DefaultPluginFileService pluginFileService;

    @BeforeMethod(groups = "fast")
    public void beforeMethod() {
        pluginFileService = new DefaultPluginFileService(TestUtils.getTestProperties());
    }

    @DataProvider(name = "createPluginDirectoryParams")
    Object[][] createPluginDirectoryParams() {
        return new Object[][] {
                { "helloworld", "1.2.1" },
                { "super-code", "1.2.3-SNAPSHOT" },
                { "CONTAINS_UPPER_CASE", "1.2.3-RC2" },
                { "helloworld", "1.2.1" } // Make sure that rewrite directory is Ok
        };
    }

    @Test(groups = "fast", dataProvider = "createPluginDirectoryParams")
    public void testCreatePluginDirectory(final String pluginKey, final String pluginVersion) throws IOException {
        final Path pluginDirectory = pluginFileService.createPluginDirectory(pluginKey, pluginVersion);
        final String actualVersion = PluginNamingResolver.getVersionFromString(pluginVersion);

        Assert.assertEquals(pluginDirectory, TestUtils.getTestPath("plugins", "java", pluginKey + "-plugin", actualVersion));
        Assert.assertTrue(Files.isDirectory(pluginDirectory));
    }

    @Test(groups = "fast")
    public void testCreateSymlink() throws IOException {
        final Path pluginDirectory = TestUtils.getTestPath("plugins", "symlink-test");

        final Path symlink = pluginDirectory.resolveSibling(DefaultPluginFileService.DEFAULT_SYMLINK_NAME);


        pluginFileService.createSymlink(pluginDirectory);

        Assert.assertTrue(Files.isSymbolicLink(symlink));
        Assert.assertEquals(Files.readSymbolicLink(symlink), pluginDirectory);
    }
}

