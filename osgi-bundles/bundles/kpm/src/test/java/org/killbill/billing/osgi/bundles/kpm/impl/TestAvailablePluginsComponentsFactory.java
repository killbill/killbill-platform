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

import java.nio.file.Path;
import java.util.Set;

import org.killbill.billing.osgi.api.OSGIKillbill;
import org.killbill.billing.osgi.bundles.kpm.KpmProperties;
import org.killbill.billing.osgi.bundles.kpm.PluginsDirectoryDAO;
import org.killbill.billing.osgi.bundles.kpm.PluginsDirectoryDAO.PluginsDirectoryModel;
import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.TestUtils;
import org.killbill.billing.osgi.bundles.kpm.VersionsProvider;
import org.killbill.billing.util.nodes.KillbillNodesApi;
import org.killbill.billing.util.nodes.NodeInfo;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestAvailablePluginsComponentsFactory {

    private static final String KILLBILL_POM_REGEX = ".killbill-\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9]+)?\\.pom$";

    private Path killbillPomXml;
    private Path ossParentPomXml;
    private Path mavenMetadataXml;
    private Path pluginDirectoryYml;
    private NodeInfo nodeInfo;
    private AvailablePluginsComponentsFactory componentsFactory;

    @BeforeMethod(groups = "fast")
    public void beforeMethod() throws Exception {
        killbillPomXml = TestUtils.copyTestResourceToTemp("xml", "killbill-pom.xml");
        ossParentPomXml = TestUtils.copyTestResourceToTemp("xml", "ossparent-pom.xml");
        mavenMetadataXml = TestUtils.copyTestResourceToTemp("xml", "maven-metadata.xml");

        this.pluginDirectoryYml = TestUtils.copyTestResourceToTemp("/yaml/plugins_directory.yml");

        nodeInfo = Mockito.mock(NodeInfo.class);

        final KillbillNodesApi killbillNodesApi = Mockito.mock(KillbillNodesApi.class);
        Mockito.when(killbillNodesApi.getCurrentNodeInfo()).thenReturn(nodeInfo);

        final OSGIKillbill osgiKillbill = Mockito.mock(OSGIKillbill.class);
        Mockito.when(osgiKillbill.getKillbillNodesApi()).thenReturn(killbillNodesApi);

        final KPMClient httpClient = Mockito.mock(KPMClient.class);
        // There's 2 way to get plugins_directory.yml. See DefaultPluginsDirectoryDAO.downloadPluginDirectory()
        Mockito.when(httpClient.downloadToTempOS(Mockito.contains("plugins_directory"), Mockito.anyString(), Mockito.anyString())).thenReturn(pluginDirectoryYml);
        Mockito.when(httpClient.downloadToTempOS(Mockito.contains("plugins_directory"), Mockito.anyMap(), Mockito.anyString(), Mockito.anyString())).thenReturn(pluginDirectoryYml);

        // There's 2 way to get maven-metadata.xml. See DefaultNexusMetadataFiles.getMavenMetadataXml()
        Mockito.when(httpClient.downloadToTempOS(Mockito.contains("maven-metadata"), Mockito.anyString(), Mockito.anyString())).thenReturn(mavenMetadataXml);
        Mockito.when(httpClient.downloadToTempOS(Mockito.contains("maven-metadata"), Mockito.anyMap(), Mockito.anyString(), Mockito.anyString())).thenReturn(mavenMetadataXml);

        Mockito.when(httpClient.downloadToTempOS(Mockito.contains("killbill-oss-parent"), Mockito.anyMap(), Mockito.anyString(), Mockito.anyString())).thenReturn(ossParentPomXml);
        Mockito.when(httpClient.downloadToTempOS(Mockito.matches(KILLBILL_POM_REGEX), Mockito.anyMap(), Mockito.anyString(), Mockito.anyString())).thenReturn(killbillPomXml);

        componentsFactory = new AvailablePluginsComponentsFactory(osgiKillbill, httpClient, new KpmProperties(TestUtils.getTestProperties()));
    }

    @AfterMethod(groups = "fast")
    public void afterMethod() {
        FilesUtils.deleteIfExists(killbillPomXml);
        FilesUtils.deleteIfExists(ossParentPomXml);
        FilesUtils.deleteIfExists(mavenMetadataXml);
        FilesUtils.deleteIfExists(pluginDirectoryYml);
    }

    @Test(groups = "fast")
    public void testCreateVersionsProviderWithoutNodeInfo() {
        Mockito.when(nodeInfo.getKillbillVersion()).thenReturn("0.0.0");
        // Version not actually needed here, since all pom xml mocked. Just 'LATEST' or sem-ver compliance value
        final VersionsProvider versionsProvider = componentsFactory.createVersionsProvider("laTEst", true);

        // return all version that defined in test/resources/xml/killbill-pom.xml and test/resources/xml/ossparent-pom.xml
        Assert.assertEquals(versionsProvider.getFixedKillbillVersion(), "0.24.1-SNAPSHOT");
        Assert.assertEquals(versionsProvider.getOssParentVersion(), "0.146.6");
        Assert.assertEquals(versionsProvider.getKillbillApiVersion(), "0.54.0");
        Assert.assertEquals(versionsProvider.getKillbillPluginApiVersion(), "0.27.0");
        Assert.assertEquals(versionsProvider.getKillbillCommonsVersion(), "0.25.1-209ec51-SNAPSHOT");
        Assert.assertEquals(versionsProvider.getKillbillPlatformVersion(), "0.41.0-1461460-SNAPSHOT");
    }

    @Test(groups = "fast")
    public void testCreateVersionsProviderWithNodeInfo() {
        Mockito.when(nodeInfo.getKillbillVersion()).thenReturn("0.24.1-SNAPSHOT");
        Mockito.when(nodeInfo.getApiVersion()).thenReturn("1.0-node-info");
        Mockito.when(nodeInfo.getPluginApiVersion()).thenReturn("1.0-node-info");
        Mockito.when(nodeInfo.getCommonVersion()).thenReturn("1.0-node-info");
        Mockito.when(nodeInfo.getPlatformVersion()).thenReturn("1.0-node-info");

        // Intentionally set the version the same as nodeInfo version.
        final VersionsProvider versionsProvider = componentsFactory.createVersionsProvider("0.24.1-SNAPSHOT", true);

        // Will use node info versions instead
        Assert.assertEquals(versionsProvider.getFixedKillbillVersion(), "0.24.1-SNAPSHOT");
        // This is where 'NexusMetadataFiles' still used to get killbill pom.xml info from remote.
        Assert.assertEquals(versionsProvider.getOssParentVersion(), "0.146.6");

        // The rest will use NodeInfo version
        Assert.assertEquals(versionsProvider.getKillbillApiVersion(), "1.0-node-info");
        Assert.assertEquals(versionsProvider.getKillbillPluginApiVersion(), "1.0-node-info");
        Assert.assertEquals(versionsProvider.getKillbillCommonsVersion(), "1.0-node-info");
        Assert.assertEquals(versionsProvider.getKillbillPlatformVersion(), "1.0-node-info");
    }

    @Test(groups = "fast")
    public void testCreatePluginsDirectoryDAO() throws KPMPluginException {
        // This test will return plugins with its version as long as createPluginsDirectoryDAO() supplied
        // with <MAJOR.MINOR> killbill version listed in mocked plugins_directory.yml
        final PluginsDirectoryDAO pluginsDirectoryDAO = componentsFactory.createPluginsDirectoryDAO("0.24.2-SNAPSHOT", true);
        Assert.assertNotNull(pluginsDirectoryDAO);

        final Set<PluginsDirectoryModel> plugins = pluginsDirectoryDAO.getPlugins();
        Assert.assertNotNull(plugins);
        Assert.assertFalse(plugins.isEmpty());
    }
}
