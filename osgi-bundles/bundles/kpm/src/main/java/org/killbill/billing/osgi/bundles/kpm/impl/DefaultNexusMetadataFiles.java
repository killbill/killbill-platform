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
import org.killbill.billing.osgi.bundles.kpm.UriResolver;
import org.killbill.billing.osgi.bundles.kpm.UriResolver.AuthenticationMethod;
import org.killbill.commons.utils.annotation.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultNexusMetadataFiles implements NexusMetadataFiles {

    private final Logger logger = LoggerFactory.getLogger(DefaultNexusMetadataFiles.class);

    private final KPMClient httpClient;
    private final UriResolver uriResolver;
    private final String mavenMetadataXmlUrl;
    private final String killbillVersionOrLatest;

    protected Path killbillPomXml;
    protected Path ossParentPomXml;

    DefaultNexusMetadataFiles(final KPMClient httpClient,
                              final UriResolver uriResolver,
                              final String mavenMetadataXmlUrl,
                              final String killbillVersionOrLatest) {
        this.httpClient = httpClient;
        this.uriResolver = uriResolver;
        this.mavenMetadataXmlUrl = mavenMetadataXmlUrl;
        this.killbillVersionOrLatest = killbillVersionOrLatest;

        logger.debug("Will get killbill info from sonatype repository with basePath:{}, and killbillVersion:{}", uriResolver.getBaseUri(), killbillVersionOrLatest);
    }

    @Override
    public Path getKillbillPomXml() throws Exception {
        if (killbillPomXml == null) {
            final String killbillUrl = getKillbillUrl();
            killbillPomXml = httpClient.downloadToTempOS(killbillUrl, uriResolver.getHeaders(), "killbill-pom", ".pom");
            logger.debug("getKillbillPomXml() will download to: {} from killbillUrl:{}", killbillPomXml, killbillUrl);
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
            ossParentPomXml = httpClient.downloadToTempOS(getOssParentUrl(), uriResolver.getHeaders(), "oss-parent-pom", ".pom");
        }
        return ossParentPomXml;
    }

    @Override
    public void cleanup() {
        FilesUtils.deleteIfExists(killbillPomXml);
        FilesUtils.deleteIfExists(ossParentPomXml);
    }

    @VisibleForTesting
    String getKillbillUrl() throws Exception {
        final String actualKbVersion;
        if ("latest".equalsIgnoreCase(killbillVersionOrLatest)) {
            final Path mavenMetadataXmlPath = getMavenMetadataXml();
            final XmlParser mavenMetadataParser = new XmlParser(mavenMetadataXmlPath);
            actualKbVersion = mavenMetadataParser.getValue("/versioning/latest");
            FilesUtils.deleteIfExists(mavenMetadataXmlPath);
        } else {
            actualKbVersion = killbillVersionOrLatest;
        }
        return String.format("%s/org/kill-bill/billing/killbill/%s/killbill-%s.pom", uriResolver.getBaseUri(), actualKbVersion, actualKbVersion);
    }

    @VisibleForTesting
    Path getMavenMetadataXml() {
        final String[] fileNameAndExt = {"maven-metadata", ".xml"};
        // Do not send any header if it is public repository
        if (uriResolver.getAuthMethod() == AuthenticationMethod.NONE ||
            mavenMetadataXmlUrl.contains("repo1.maven.org") ||
            mavenMetadataXmlUrl.contains("oss.sonatype.org")) {
            return httpClient.downloadToTempOS(mavenMetadataXmlUrl, fileNameAndExt);
        } else {
            return httpClient.downloadToTempOS(mavenMetadataXmlUrl, uriResolver.getHeaders(), fileNameAndExt);
        }
    }

    @VisibleForTesting
    String getOssParentUrl() throws Exception {
        if (killbillPomXml == null) {
            killbillPomXml = getKillbillPomXml();
        }
        final XmlParser xmlParser = new XmlParser(killbillPomXml);
        final String ossVersion = xmlParser.getValue("/parent/version");
        return String.format("%s/org/kill-bill/billing/killbill-oss-parent/%s/killbill-oss-parent-%s.pom", uriResolver.getBaseUri(), ossVersion, ossVersion);
    }
}
