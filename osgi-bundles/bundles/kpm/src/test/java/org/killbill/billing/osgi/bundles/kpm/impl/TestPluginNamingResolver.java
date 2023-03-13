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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestPluginNamingResolver {

    @DataProvider(name = "createGetVersionFromStringParams")
    Object[][] createGetVersionFromStringParams() {
        return new Object[][] {
                { "plugin-a-1.2.1.jar", "1.2.1" },
                { "plugin-b-1.2.3-SNAPSHOT.jar", "1.2.3" },
                { "my.super.plugin.1.2.3-snapshot.jar", "1.2.3" }
        };
    }

    @Test(groups = "fast", dataProvider = "createGetVersionFromStringParams")
    public void testGetVersionFromString(final String version, final String expectedVersion) {
        final String result = PluginNamingResolver.getVersionFromString(version);
        Assert.assertEquals(result, expectedVersion);
    }

    @DataProvider(name = "createGetPluginNameParams")
    Object[][] createGetPluginNameParams() {
        return new Object[][] {
                { "helloworld", "helloworld-plugin" },
                { "super-plugin", "super-plugin-plugin" }, // this is what we have in KPM
                { "this-is-plugin-that-actually-not-plugin", "this-is-plugin-that-actually-not-plugin-plugin" } // see above
        };
    }

    @Test(groups = "fast", dataProvider = "createGetPluginNameParams")
    public void testGetPluginName(final String pluginKey, final String expectedPluginName) {
        final PluginNamingResolver pluginNamingResolver = PluginNamingResolver.of(pluginKey, "0.0.0"); // version not actually needed
        Assert.assertEquals(pluginNamingResolver.getPluginName(), expectedPluginName);
    }

    @DataProvider(name = "createGetPluginVersionParams")
    Object[][] createGetPluginVersionParams() {
        return new Object[][] {
                { "1.0.0-SNAPSHOT", "not-needed", "1.0.0" },
                { "not-a-version", "https://maven.company.com/releases/com/company/killbill/plugins/superplugin/superplugin-1.2.3.jar", "1.2.3" },
                { "not-a-version", "https://myapp.com/no-version-involved", "0.0.0" }
        };
    }

    @Test(groups = "fast", dataProvider = "createGetPluginVersionParams")
    public void testGetPluginVersion(final String pluginVersion, final String strContainsVersion, final String expectedVersion) {
        // pluginKey not needed here.
        final PluginNamingResolver namingResolver = PluginNamingResolver.of("helloworld", pluginVersion, strContainsVersion);
        Assert.assertEquals(namingResolver.getPluginVersion(), expectedVersion);
    }

    @DataProvider(name = "createGetPluginJarFileNameParams")
    Object[][] createGetPluginJarFileNameParams() {
        return new Object[][] {
                { "helloworld", "1.2.3", "helloworld-plugin-1.2.3.jar" },
                { "super-plugin", "1.1.2", "super-plugin-plugin-1.1.2.jar" }, // this is what we have in KPM
                { "snapshot-test", "9.0.1-SNAPSHOT", "snapshot-test-plugin-9.0.1.jar" }
        };
    }

    @Test(groups = "fast", dataProvider = "createGetPluginJarFileNameParams")
    public void testGetPluginJarFileName(final String pluginKey, final String version, final String expectedJarFileName) {
        final PluginNamingResolver pluginNamingResolver = PluginNamingResolver.of(pluginKey, version);
        Assert.assertEquals(pluginNamingResolver.getPluginJarFileName(), expectedJarFileName);
    }
}
