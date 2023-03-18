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

import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.killbill.billing.osgi.bundles.kpm.NexusMetadataFiles;
import org.killbill.commons.utils.annotation.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SonatypeNexusMetadataFiles implements NexusMetadataFiles {

    private final Logger logger = LoggerFactory.getLogger(SonatypeNexusMetadataFiles.class);

    private final KPMClient httpClient;

    protected Path killbillPomXml;
    protected Path ossParentPomXml;
    private final String basePath;
    private final String killbillVersionOrLatest;

    SonatypeNexusMetadataFiles(final KPMClient httpClient,
                               final String nexusUrl,
                               final String nexusRepository,
                               final String killbillVersionOrLatest) {
        this.httpClient = httpClient;
        this.basePath = String.format("%s/content/repositories/%s", nexusUrl, nexusRepository);
        this.killbillVersionOrLatest = killbillVersionOrLatest;

        logger.debug("Will get killbill info from sonatype repository with basePath:{}, and killbillVersion:{}", basePath, killbillVersionOrLatest);
    }

    @Override
    public Path getKillbillPomXml() throws Exception {
        if (killbillPomXml == null) {
            final String killbillUrl = getKillbillUrl();
            logger.debug("getKillbillPomXml() with killbillUrl:{}", killbillUrl);
            killbillPomXml = httpClient.downloadArtifactMetadata(killbillUrl);
        } else {
            logger.debug("getKillbillPomXml() is not null and the value is:{}", killbillPomXml);
        }
        return killbillPomXml;
    }

    @Override
    public Path getOssParentPomXml() throws Exception {
        if (ossParentPomXml == null) {
            if (killbillPomXml == null) {
                killbillPomXml = getKillbillPomXml();
            }
            ossParentPomXml = httpClient.downloadArtifactMetadata(getOssParentUrl());
        }
        return ossParentPomXml;
    }

    @VisibleForTesting
    String getKillbillUrl() throws Exception {
        final String actualKbVersion;
        if ("latest".equalsIgnoreCase(killbillVersionOrLatest)) {
            final Path mavenMetadataXmlPath = httpClient.downloadArtifactMetadata(getMavenMetadataXmlUrl());
            final XmlParser mavenMetadataParser = new XmlParser(mavenMetadataXmlPath);
            actualKbVersion = mavenMetadataParser.getValue("/versioning/latest");
        } else {
            actualKbVersion = killbillVersionOrLatest;
        }
        return String.format("%s/org/kill-bill/billing/killbill/%s/killbill-%s.pom", basePath, actualKbVersion, actualKbVersion);
    }

    @VisibleForTesting
    String getMavenMetadataXmlUrl() {
        return basePath + "/org/kill-bill/billing/killbill/maven-metadata.xml";
    }

    @VisibleForTesting
    String getOssParentUrl() throws Exception {
        if (killbillPomXml == null) {
            killbillPomXml = getKillbillPomXml();
        }
        final XmlParser xmlParser = new XmlParser(killbillPomXml);
        final String ossVersion = xmlParser.getValue("/parent/version");
        return String.format("%s/org/kill-bill/billing/killbill-oss-parent/%s/killbill-oss-parent-%s.pom", basePath, ossVersion, ossVersion);
    }
}
