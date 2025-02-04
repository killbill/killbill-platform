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

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.KpmProperties;
import org.killbill.billing.osgi.bundles.kpm.PluginIdentifiersDAO;
import org.killbill.billing.osgi.bundles.kpm.PluginIdentifiersDAO.PluginIdentifierModel;
import org.killbill.billing.osgi.bundles.kpm.TestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestFileBasedPluginIdentifiersDAO {

    private FileBasedPluginIdentifiersDAO pluginIdentifiersDAO;

    @BeforeMethod(groups = "fast")
    public void beforeMethod() {
        pluginIdentifiersDAO = new FileBasedPluginIdentifiersDAO(new KpmProperties(TestUtils.getTestProperties()));
    }

    @AfterMethod(groups = "fast")
    public void afterMethod() {
        pluginIdentifiersDAO.file.delete();
    }

    @Test(groups = "fast")
    public void testAdd() {
        Map<String, PluginIdentifierModel> content = pluginIdentifiersDAO.loadFileContent();
        Assert.assertEquals(content.size(), 0);

        final String pluginKey = "testPlugin";
        final String version = "1.0";
        final PluginNamingResolver pluginNamingResolver = PluginNamingResolver.of(pluginKey, version);

        pluginIdentifiersDAO.add(pluginKey, version);

        content = pluginIdentifiersDAO.loadFileContent();
        final PluginIdentifierModel pluginIdentifierModel = content.get(pluginKey);
        Assert.assertEquals(content.size(), 1);
        Assert.assertNotNull(pluginIdentifierModel);
        Assert.assertEquals(pluginIdentifierModel.getPluginName(), pluginNamingResolver.getPluginName());
        Assert.assertEquals(pluginIdentifierModel.getVersion(), pluginNamingResolver.getPluginVersion());
    }

    @Test(groups = "fast", expectedExceptions = KPMPluginException.class)
    void testLoadFileContentInvalidJson() throws IOException {
        try (final FileWriter writer = new FileWriter(pluginIdentifiersDAO.file, false)) {
            writer.write("INVALID_JSON_CONTENT");
        }

        pluginIdentifiersDAO.loadFileContent();
    }

    @Test(groups = "fast")
    void testLoadFileContentEmptyJson() throws IOException {
        try (final FileWriter writer = new FileWriter(pluginIdentifiersDAO.file)) {
            writer.write(" ");
        }

        final Map<String, PluginIdentifiersDAO.PluginIdentifierModel> pluginIdentifierModelMap = pluginIdentifiersDAO.loadFileContent();

        Assert.assertTrue(pluginIdentifierModelMap.isEmpty());
    }

    @Test(groups = "fast")
    public void testRemove() {
        final String pluginKey = "testPlugin";
        final String version = "1.0";
        pluginIdentifiersDAO.add(pluginKey, version);

        pluginIdentifiersDAO.remove(pluginKey);

        final Map<String, PluginIdentifierModel> content = pluginIdentifiersDAO.loadFileContent();
        Assert.assertEquals(content.size(), 0);
    }
}

