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
import org.killbill.billing.osgi.bundles.kpm.UriResolver;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Slow test, because otherwise, too much mocking of {@code KPMClient} and error-prone.
 */
public class TestDefaultNexusMetadataFiles {

    private static final String MAVEN_METADATA_XML = "https://repo1.maven.org/maven2/org/kill-bill/billing/killbill/maven-metadata.xml";

    private DefaultNexusMetadataFiles nexusMetadataFiles;
    private KPMClient kpmClient;

    @BeforeMethod(groups = "slow")
    public void beforeMethod() throws Exception {
        kpmClient = new KPMClient(true, 30000, 30000);
    }

    @AfterMethod(groups = "slow")
    public void afterMethod() {
        if (nexusMetadataFiles != null) {
            nexusMetadataFiles.cleanup();
        } else {
            throw new IllegalStateException("Due to previous error, please clean your OS temp directory manually");
        }
    }

    private DefaultNexusMetadataFiles spyNexusMetadataFiles(final KPMClient kpmClient,
                                                            final UriResolver uriResolver,
                                                            final String mavenMetadataXmlUrl,
                                                            final String kbVersion) {
        final DefaultNexusMetadataFiles toSpy = new DefaultNexusMetadataFiles(kpmClient, uriResolver, mavenMetadataXmlUrl, kbVersion);
        return Mockito.spy(toSpy);
    }

    private void testGetKillbillPomXml(final String kbVersion) throws Exception {
        final UriResolver uriResolver = new NoneUriResolver("https://oss.sonatype.org/content/repositories/releases");

        nexusMetadataFiles = spyNexusMetadataFiles(kpmClient, uriResolver, MAVEN_METADATA_XML, kbVersion);
        final Path killbillPom = nexusMetadataFiles.getKillbillPomXml();
        final String content = Files.readString(killbillPom);

        Assert.assertTrue(content.contains("<artifactId>killbill</artifactId>"));

        FilesUtils.deleteIfExists(killbillPom);
    }

    @Test(groups = "slow")
    public void testGetKillbillPomXmlWithLatest() throws Exception {
        testGetKillbillPomXml("latest");

        Mockito.verify(nexusMetadataFiles, Mockito.times(1)).getKillbillUrl();
        Mockito.verify(nexusMetadataFiles, Mockito.times(1)).getMavenMetadataXml();
    }

    @Test(groups = "slow")
    public void testGetKillbillPomXmlWithFixedVersion() throws Exception {
        testGetKillbillPomXml("0.24.0");

        Mockito.verify(nexusMetadataFiles, Mockito.times(1)).getKillbillUrl();
        Mockito.verify(nexusMetadataFiles, Mockito.times(0)).getMavenMetadataXml();
    }

    private void testGetOssParentPomXml(final String kbVersion) throws Exception {
        final UriResolver uriResolver = new NoneUriResolver("https://oss.sonatype.org/content/repositories/releases");

        nexusMetadataFiles = spyNexusMetadataFiles(kpmClient, uriResolver, MAVEN_METADATA_XML, kbVersion);
        final Path ossParentPomXml = nexusMetadataFiles.getOssParentPomXml();
        final String content = Files.readString(ossParentPomXml);

        Assert.assertTrue(content.contains("<artifactId>killbill-oss-parent</artifactId>"));

        FilesUtils.deleteIfExists(ossParentPomXml);
    }

    @Test(groups = "slow")
    public void testGetOssParentWithLatest() throws Exception {
        testGetOssParentPomXml("latest");

        Mockito.verify(nexusMetadataFiles, Mockito.times(1)).getKillbillPomXml();
        Mockito.verify(nexusMetadataFiles, Mockito.times(1)).getKillbillUrl();
        Mockito.verify(nexusMetadataFiles, Mockito.times(1)).getMavenMetadataXml();
    }

    @Test(groups = "slow")
    public void testGetOssParentWithFixedVersion() throws Exception {
        testGetOssParentPomXml("0.24.1");

        Mockito.verify(nexusMetadataFiles, Mockito.times(1)).getKillbillPomXml();
        Mockito.verify(nexusMetadataFiles, Mockito.times(1)).getKillbillUrl();
        Mockito.verify(nexusMetadataFiles, Mockito.never()).getMavenMetadataXml();
    }
}
