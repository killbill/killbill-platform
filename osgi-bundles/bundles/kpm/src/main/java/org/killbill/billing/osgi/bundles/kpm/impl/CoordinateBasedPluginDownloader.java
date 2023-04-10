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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarFile;

import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.PluginManager;
import org.killbill.billing.osgi.bundles.kpm.impl.ArtifactAndVersionFinder.ArtifactAndVersionModel;
import org.killbill.commons.utils.Preconditions;
import org.killbill.commons.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add ability to download plugin based on (maven) coordinate (groupId, artifactId, version).
 */
class CoordinateBasedPluginDownloader {

    private static final Logger logger = LoggerFactory.getLogger(CoordinateBasedPluginDownloader.class);

    private static final String KILLBILL_GROUP_ID = "org.kill-bill.billing.plugin.java";

    private static final String SONATYPE_RELEASE_URL = "https://oss.sonatype.org/content/repositories/releases";

    private final KPMClient httpClient;
    private final Sha1Checker sha1Checker;
    private final ArtifactAndVersionFinder artifactAndVersionFinder;

    private final String pluginRepository;

    CoordinateBasedPluginDownloader(final KPMClient httpClient, final ArtifactAndVersionFinder artifactAndVersionFinder, final Properties properties) {
        this.httpClient = httpClient;
        this.sha1Checker = new Sha1Checker(httpClient, properties);
        this.pluginRepository = getPluginRepository(properties);

        this.artifactAndVersionFinder = artifactAndVersionFinder;
    }

    DownloadResult download(final String pluginKey,
                            final String killbillVersion,
                            final String pluginGroupId,
                            final String pluginArtifactId,
                            final String pluginVersion,
                            final boolean forceDownload) throws KPMPluginException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(pluginKey), "'pluginKey' is null or empty.");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(killbillVersion), "'killbillVersion' is null or empty.");
        Preconditions.checkArgument(!"latest".equalsIgnoreCase(killbillVersion), "PluginDownloader not accepting 'latest' killbill version");

        // See ArtifactAndVersionFinder javadocs for why we need this.
        final Optional<ArtifactAndVersionModel> artifactAndVersionOpt = artifactAndVersionFinder.findArtifactAndVersion(
                killbillVersion,
                pluginKey,
                pluginArtifactId,
                pluginVersion,
                forceDownload);

        if (artifactAndVersionOpt.isEmpty()) {
            // There's no need to search remotely. We don't know exact plugin artifact and version to find.
            throw new KPMPluginException("Unable to find 'pluginArtifactId' and/or 'pluginVersion'. This is required when install plugin via coordinate");
        }

        final ArtifactAndVersionModel artifactAndVersion = artifactAndVersionOpt.get();

        final String actualGroupId = Objects.requireNonNullElse(pluginGroupId, KILLBILL_GROUP_ID);
        final String actualArtifactId = artifactAndVersion.getArtifactId();
        final String actualPluginVersion = artifactAndVersion.getVersion();

        logger.debug("Try to download plugin from nexus URL");
        Path result = downloadFromNexusUri(pluginKey, actualGroupId, actualArtifactId, actualPluginVersion);

        if (result != null && Files.exists(result)) {
            logger.debug("Plugin found in nexus URL: {}", result);
            return new DownloadResult(result, actualGroupId, actualArtifactId, actualPluginVersion);
        }
        logger.debug("Plugin not found in nexus URL. Will try killbill public repository");
        result = downloadFromPublicKillbill(pluginKey, actualGroupId, actualArtifactId, actualPluginVersion);
        if (result != null && Files.exists(result)) {
            logger.debug("Plugin found in killbill public repository: {}", result);
            return new DownloadResult(result, actualGroupId, actualArtifactId, actualPluginVersion);
        }

        final String msg = String.format("Cannot find plugin with key: %s, kbVersion: %s, groupId: %s, artifactId: %s, version: %s. " +
                                         "Note that at this point, groupId/artifactId/version might different with input due to search " +
                                         "operation by artifactAndVersionFinder.findArtifactAndVersion()",
                                         pluginKey, killbillVersion, actualGroupId, actualArtifactId, actualPluginVersion);

        throw new KPMPluginException(msg);
    }

    private String getPluginRepository(final Properties properties) {
        final String pluginInstallRepoUrl = properties.getProperty(PluginManager.PROPERTY_PREFIX + "pluginInstall.pluginRepositoryUrl");
        // Attempt to get value from
        if (Strings.isNullOrEmpty(pluginInstallRepoUrl)) {
            final String nexusUrl = Objects.requireNonNullElse(properties.getProperty(PluginManager.PROPERTY_PREFIX + "nexusUrl"), "https://oss.sonatype.org");
            final String nexusRepo = Objects.requireNonNullElse(properties.getProperty(PluginManager.PROPERTY_PREFIX + "nexusRepository"), "content/repositories/releases");
            return nexusUrl + "/" + nexusRepo;
        }
        return pluginInstallRepoUrl;
    }

    private Path doDownloadValidateAndVerify(final String pluginKey, final String version, final String pluginJarUrl, final String sha1Url) {
        try {
            // FIXME-93: This is not support github and cloudsmith
            final String fileName = PluginNamingResolver.of(pluginKey, version).getPluginJarFileName();
            final Path downloadedFile = Files.createTempDirectory("kpm").resolve(fileName);
            httpClient.downloadPlugin(pluginJarUrl, downloadedFile);

            if (!Files.exists(downloadedFile) && Files.size(downloadedFile) <= 0L) {
                throw new KPMPluginException(String.format("Unable to download file named as '%s' from %s", fileName, pluginKey));
            }
            if (!isValidJarFile(downloadedFile)) {
                throw new KPMPluginException("Plugin downloaded successfully, but it is not a JAR file");
            } else {
                logger.debug("Downloaded plugin file is a valid JAR file");
            }
            if (!sha1Checker.isDownloadedFileVerified(sha1Url, downloadedFile)) {
                throw new KPMPluginException("Plugin downloaded successfully, but SHA1 verification failed");
            } else {
                logger.debug("Downloaded plugin file is verified");
            }
            return downloadedFile;
        } catch (final IOException e) {
            throw new KPMPluginException("Problem when creating temporary file for download plugin", e);
        } catch (final Exception e) {
            logger.error("There's problem when downloading plugin from: {}. Exception: {}", pluginJarUrl, e);
            return null;
        }
    }

    private Path downloadFromNexusUri(final String pluginKey, final String groupId, final String artifactId, final String version) {
        final String jarUrl = pluginRepository + "/" + coordinateToUri(groupId, artifactId, version, ".jar");
        final String sha1Url = pluginRepository + "/" + coordinateToUri(groupId, artifactId, version, ".jar.sha1");
        // FIXME-93: This is not support github and cloudsmith
        return doDownloadValidateAndVerify(pluginKey, version, jarUrl, sha1Url);
    }

    private Path downloadFromPublicKillbill(final String pluginKey, final String groupId, final String artifactId, final String version) {
        // If downloadFromNexusUri() already from sonatype, then do no repeat, since this method should be invoked
        // as last attempt.
        if (pluginRepository.startsWith("https://oss.sonatype.org")) {
            return null;
        }

        final String jarUrl =  SONATYPE_RELEASE_URL + coordinateToUri(groupId, artifactId, version, ".jar");
        final String sha1Url = SONATYPE_RELEASE_URL + coordinateToUri(groupId, artifactId, version, ".jar.sha1");

        return doDownloadValidateAndVerify(pluginKey, version, jarUrl, sha1Url);
    }

    static String coordinateToUri(final String groupId, final String artifactId, final String pluginVersion, final String extension) {
        return groupId.replaceAll("\\.", "/") + "/" +
               artifactId + "/" +
               pluginVersion + "/" +
               artifactId + "-" + pluginVersion + extension;
    }

    static boolean isValidJarFile(final Path path) {
        try (final JarFile jarFile = new JarFile(path.toFile())) {
            logger.debug("Actual version from downloaded plugin JAR file: {}", jarFile.getVersion());
            return true;
        } catch (final IOException e) {
            logger.debug("Downloaded plugin: {} is not a JAR file", path);
            return false;
        }
    }

    // Needed by DefaultPluginManager -> URIBasedPluginInstaller. It requires valid, non-null pluginVersion to work.
    // Also Needed by DefaultPluginManager -> pluginIdentifiersDAO.java:108 . It requires non-null groupId and artifactId
    static class DownloadResult {
        private final Path downloadedPath;
        private final String groupId;
        private final String artifactId;
        private final String pluginVersion;

        DownloadResult(final Path downloadedPath, final String groupId, final String artifactId, final String pluginVersion) {
            this.downloadedPath = downloadedPath;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.pluginVersion = pluginVersion;
        }

        public Path getDownloadedPath() {
            return downloadedPath;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getPluginVersion() {
            return pluginVersion;
        }

        @Override
        public String toString() {
            return "DownloadResult{" +
                   "downloadedPath=" + downloadedPath +
                   ", artifactId='" + artifactId + '\'' +
                   ", pluginVersion='" + pluginVersion + '\'' +
                   '}';
        }
    }
}
