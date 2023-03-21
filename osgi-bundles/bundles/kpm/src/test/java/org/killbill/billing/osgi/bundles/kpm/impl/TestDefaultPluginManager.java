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

import java.util.Properties;

import org.killbill.billing.osgi.bundles.kpm.GetAvailablePluginsModel;
import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.PluginFileService;
import org.killbill.billing.osgi.bundles.kpm.PluginIdentifierService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

// Slow test because DefaultPluginManager operations will call remote HTTP call
public class TestDefaultPluginManager {

    private DefaultPluginManager pluginManager;

    @BeforeMethod(groups = "slow")
    public void beforeMethod() {
        final OSGIKillbillAPI osgiKillbillAPI = Mockito.mock(OSGIKillbillAPI.class);
        final Properties properties = new Properties();

        final PluginFileService pluginFileService = new DefaultPluginFileService(properties);
        final PluginIdentifierService pluginIdentifierService = new DefaultPluginIdentifierService(properties);

        final DefaultPluginManager toSpy = new DefaultPluginManager(osgiKillbillAPI, properties);
        pluginManager = Mockito.spy(toSpy);
        Mockito.doReturn(pluginFileService).when(pluginManager).createPluginFileService(properties);
        Mockito.doReturn(pluginIdentifierService).when(pluginManager).createPluginIdentifierService(properties);
    }

    @Test(groups = "slow")
    public void testGetAvailablePlugins() {
        // Get plugin info from actual, default, killbill plugins_directory.yml. See DefaultAvailablePluginsProvider
        GetAvailablePluginsModel result = pluginManager.getAvailablePlugins("0.18.0", true);
        Assert.assertEquals(result.getKillbillArtifactsVersion().getKillbill(), "0.18.0");
        Assert.assertFalse(result.getAvailablePlugins().isEmpty());

        try {
            result = pluginManager.getAvailablePlugins("not-exist", true);
            Assert.fail();
        } catch (final KPMPluginException e) {
            Assert.assertTrue(e.getMessage().contains("Unable to get killbill version info"));
        }

        // will never throw an exception
        result = pluginManager.getAvailablePlugins("not-exist", false);
        // will return VersionsProvider.ZERO
        Assert.assertEquals(result.getKillbillArtifactsVersion().getKillbill(), "0.0.0");
        // will return AvailablePluginsProvider.NONE
        Assert.assertTrue(result.getAvailablePlugins().isEmpty());
    }

}
