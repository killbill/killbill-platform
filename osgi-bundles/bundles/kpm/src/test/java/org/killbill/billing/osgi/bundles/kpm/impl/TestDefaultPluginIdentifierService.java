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

import org.killbill.billing.osgi.bundles.kpm.PluginIdentifier;
import org.killbill.billing.osgi.bundles.kpm.TestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

public class TestDefaultPluginIdentifierService {

    private DefaultPluginIdentifierService pluginIdentifierService;

    @BeforeMethod(groups = "fast")
    public void beforeMethod() {
        pluginIdentifierService = new DefaultPluginIdentifierService(TestUtils.getTestProperties());
    }

    @AfterMethod(groups = "fast")
    public void afterMethod() {
        pluginIdentifierService.file.delete();
    }

    @Test(groups = "fast")
    public void testAdd() {
        Map<String, PluginIdentifier> content = pluginIdentifierService.loadFileContent();
        Assert.assertEquals(content.size(), 0);

        final String pluginKey = "testPlugin";
        final String version = "1.0";
        final PluginNamingResolver pluginNamingResolver = PluginNamingResolver.of(pluginKey, version);

        pluginIdentifierService.add(pluginKey, version);

        content = pluginIdentifierService.loadFileContent();
        final PluginIdentifier pluginIdentifier = content.get(pluginKey);
        Assert.assertEquals(content.size(), 1);
        Assert.assertNotNull(pluginIdentifier);
        Assert.assertEquals(pluginIdentifier.getPluginName(), pluginNamingResolver.getPluginName());
        Assert.assertEquals(pluginIdentifier.getVersion(), pluginNamingResolver.getPluginVersion());
    }

    @Test(groups = "fast")
    public void testRemove() {
        final String pluginKey = "testPlugin";
        final String version = "1.0";
        pluginIdentifierService.add(pluginKey, version);

        pluginIdentifierService.remove(pluginKey);

        final Map<String, PluginIdentifier> content = pluginIdentifierService.loadFileContent();
        Assert.assertEquals(content.size(), 0);
    }
}

