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

import java.nio.file.Files;
import java.nio.file.Path;

import org.killbill.billing.osgi.bundles.kpm.AvailablePluginsProvider;
import org.killbill.billing.osgi.bundles.kpm.AvailablePluginsProvider.AvailablePluginsModel;
import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.killbill.billing.osgi.bundles.kpm.TestUtils;
import org.killbill.commons.utils.Strings;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestDefaultAvailablePluginsProvider {

    private Path pluginsDirectoryYml;

    private KPMClient httpClient;

    @BeforeMethod(groups = "fast")
    public void beforeMethod() throws Exception {
        // Assign to temporary file, so original test resources files not getting deleted.
        final Path pluginsDirYmlInTestRes = TestUtils.getTestPath("yaml").resolve("plugin_directory.yml");
        pluginsDirectoryYml = Files.createTempFile("kpm-test", "");
        if (Files.notExists(pluginsDirectoryYml)) {
            Files.copy(pluginsDirYmlInTestRes, pluginsDirectoryYml);
        }

        // In KPMClient #downloadArtifactMetadata() return tmp file instead file in src/test/resources
        httpClient = Mockito.mock(KPMClient.class);
        Mockito.when(httpClient.downloadArtifactMetadata(Mockito.anyString())).thenReturn(pluginsDirectoryYml);
    }

    @Test(groups = "fast")
    public void testGetAvailablePlugins() throws Exception {
        final AvailablePluginsProvider availablePluginsProvider = new DefaultAvailablePluginsProvider(httpClient, "0.24.0", pluginsDirectoryYml.toString());
        for (final AvailablePluginsModel availablePlugin : availablePluginsProvider.getAvailablePlugins()) {
            Assert.assertNotNull(availablePlugin);

            final String pluginKey = availablePlugin.getPluginKey();
            final String pluginVersion = availablePlugin.getPluginVersion();
            Assert.assertFalse(Strings.isNullOrEmpty(pluginKey));
            Assert.assertFalse(Strings.isNullOrEmpty(pluginVersion));
        }
    }
}
