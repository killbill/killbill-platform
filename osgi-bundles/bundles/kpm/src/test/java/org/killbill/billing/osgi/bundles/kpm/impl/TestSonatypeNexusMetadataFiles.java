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

import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.killbill.billing.osgi.bundles.kpm.TestUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestSonatypeNexusMetadataFiles {

    private static final String NEXUS_URL = "https://oss.sonatype.org";

    private final Path killbillPomXml = TestUtils.getTestPath("xml").resolve("killbill-pom.xml");
    private final Path ossParentPomXml = TestUtils.getTestPath("xml").resolve("ossparent-pom.xml");
    private final Path mavenMetadataXml = TestUtils.getTestPath("xml").resolve("maven-metadata.xml");


    private SonatypeNexusMetadataFiles sonatypeNexusMetadataFiles;

    @Test(groups = "fast")
    public void testGetKillbillPomXml() throws Exception {

        KPMClient httpClient = Mockito.mock(KPMClient.class);
        Mockito.when(httpClient.downloadArtifactMetadata(Mockito.anyString())).thenReturn(killbillPomXml);

        sonatypeNexusMetadataFiles = new SonatypeNexusMetadataFiles(httpClient, NEXUS_URL, "releases", "LATEST");
        final Path killbillPomXmlPath = sonatypeNexusMetadataFiles.getKillbillPomXml();

        Assert.assertNotNull(killbillPomXmlPath);
        Assert.assertTrue(Files.exists(killbillPomXmlPath));
        Assert.assertTrue(Files.isRegularFile(killbillPomXmlPath));

        // 2 times: getKillbillUrl() and getKillbillPomXml()
        Mockito.verify(httpClient, Mockito.times(2)).downloadArtifactMetadata(Mockito.anyString());

        // --

        httpClient = Mockito.mock(KPMClient.class);
        sonatypeNexusMetadataFiles = new SonatypeNexusMetadataFiles(httpClient, NEXUS_URL, "releases", "1.0.4-SNAPSHOT");
        sonatypeNexusMetadataFiles.getKillbillPomXml();

        // only 1x time: getKillbillPomXml(). getKillbillUrl() just return supplied version
        Mockito.verify(httpClient, Mockito.times(1)).downloadArtifactMetadata(Mockito.anyString());
    }

    @Test(groups = "fast")
    public void testGetOssParentPomXml() throws Exception {
        final KPMClient httpClient = Mockito.mock(KPMClient.class);
        Mockito.when(httpClient.downloadArtifactMetadata(Mockito.anyString())).thenReturn(ossParentPomXml);

        sonatypeNexusMetadataFiles = new SonatypeNexusMetadataFiles(httpClient, NEXUS_URL, "releases", "LATEST");

        final Path ossParentPomXmlPath = sonatypeNexusMetadataFiles.getOssParentPomXml();
        Assert.assertNotNull(ossParentPomXmlPath);
        Assert.assertTrue(Files.exists(ossParentPomXmlPath));
        Assert.assertTrue(Files.isRegularFile(ossParentPomXmlPath));

        // 3 times: getKillbillUrl(), getKillbillPomXml() and getOssParentPomXml()
        Mockito.verify(httpClient, Mockito.times(3)).downloadArtifactMetadata(Mockito.anyString());
    }

    @Test(groups = "fast")
    public void testGetMavenMetadataXmlUrl() throws Exception {
        final KPMClient httpClient = Mockito.mock(KPMClient.class);

        sonatypeNexusMetadataFiles = new SonatypeNexusMetadataFiles(httpClient, NEXUS_URL, "does-not-matter", "LATEST");
        final String result = sonatypeNexusMetadataFiles.getMavenMetadataXmlUrl();

        Assert.assertNotNull(result);
        Assert.assertEquals(
                result,
                "https://oss.sonatype.org/content/repositories/does-not-matter/org/kill-bill/billing/killbill/maven-metadata.xml");
    }

    @Test(groups = "fast")
    public void testGetKillbillUrl() throws Exception {
        KPMClient httpClient = Mockito.mock(KPMClient.class);
        Mockito.when(httpClient.downloadArtifactMetadata(Mockito.contains("maven-metadata.xml"))).thenReturn(mavenMetadataXml);

        sonatypeNexusMetadataFiles = new SonatypeNexusMetadataFiles(httpClient, NEXUS_URL, "releases", "LATEST");

        Assert.assertEquals(
                sonatypeNexusMetadataFiles.getKillbillUrl(),
                "https://oss.sonatype.org/content/repositories/releases/org/kill-bill/billing/killbill/0.24.1/killbill-0.24.1.pom");
        Mockito.verify(httpClient, Mockito.times(1)).downloadArtifactMetadata(Mockito.contains("maven-metadata.xml"));

        // --

        httpClient = Mockito.mock(KPMClient.class);
        Mockito.when(httpClient.downloadArtifactMetadata(Mockito.contains("maven-metadata.xml"))).thenReturn(mavenMetadataXml);
        Mockito.when(httpClient.downloadArtifactMetadata(Mockito.contains("killbill-"))).thenReturn(killbillPomXml);
        sonatypeNexusMetadataFiles = new SonatypeNexusMetadataFiles(httpClient, NEXUS_URL, "releases", "0.23.0");

        Assert.assertEquals(
                sonatypeNexusMetadataFiles.getKillbillUrl(),
                "https://oss.sonatype.org/content/repositories/releases/org/kill-bill/billing/killbill/0.23.0/killbill-0.23.0.pom");
        // If version set (0.23.0, see above) then don't need to find maven-metadata.xml
        Mockito.verify(httpClient, Mockito.never()).downloadArtifactMetadata(Mockito.contains("maven-metadata.xml"));
    }

    @Test(groups = "fast")
    public void testGetOssParentUrl() throws Exception {
        final KPMClient httpClient = Mockito.mock(KPMClient.class);
        // This is when maven-metadata.xml needed
        Mockito.when(httpClient.downloadArtifactMetadata(Mockito.contains("maven-metadata.xml"))).thenReturn(mavenMetadataXml);
        // .... And this is when kill-bill's pom.xml needed
        Mockito.when(httpClient.downloadArtifactMetadata(Mockito.contains("killbill-"))).thenReturn(killbillPomXml);

        sonatypeNexusMetadataFiles = new SonatypeNexusMetadataFiles(httpClient, NEXUS_URL, "releases", "LATEST");

        Assert.assertEquals(
                sonatypeNexusMetadataFiles.getOssParentUrl(),
                "https://oss.sonatype.org/content/repositories/releases/org/kill-bill/billing/killbill-oss-parent/0.146.6/killbill-oss-parent-0.146.6.pom");
    }
}
