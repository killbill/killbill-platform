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
import java.util.jar.JarFile;

import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.UriResolver;
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

    private static final String SONATYPE_RELEASE_URL = "https://oss.sonatype.org/content/repositories/releases/";

    private final KPMClient httpClient;
    private final Sha1Checker sha1Checker;
    private final ArtifactAndVersionFinder artifactAndVersionFinder;
    private final UriResolver uriResolver;
    private final boolean shouldTryPublicRepository;

    CoordinateBasedPluginDownloader(final KPMClient httpClient,
                                    final ArtifactAndVersionFinder artifactAndVersionFinder,
                                    final UriResolver uriResolver,
                                    final boolean shouldVerify,
                                    final boolean shouldTryPublicRepository) {
        this.httpClient = httpClient;
        this.artifactAndVersionFinder = artifactAndVersionFinder;
        this.uriResolver = uriResolver;
        this.sha1Checker = new Sha1Checker(httpClient, shouldVerify);
        this.shouldTryPublicRepository = shouldTryPublicRepository;
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
            // There's no need to try download remotely. We don't know exact plugin artifact and version to download.
            throw new KPMPluginException("Unable to find 'pluginArtifactId' and/or 'pluginVersion'. This is required when install plugin via coordinate");
        }

        final ArtifactAndVersionModel artifactAndVersion = artifactAndVersionOpt.get();

        final String actualGroupId = Objects.requireNonNullElse(pluginGroupId, KILLBILL_GROUP_ID);
        final String actualArtifactId = artifactAndVersion.getArtifactId();
        final String actualPluginVersion = artifactAndVersion.getVersion();

        Path result = downloadFromPluginInstallUrl(pluginKey, actualGroupId, actualArtifactId, actualPluginVersion);
        if (result != null && Files.exists(result)) {
            logger.debug("Plugin found in pluginInstall.coordinate.url. Downloaded path: {}", result);
            return new DownloadResult(result, actualGroupId, actualArtifactId, actualPluginVersion);
        }

        result = downloadFromPublicKillbill(pluginKey, actualGroupId, actualArtifactId, actualPluginVersion);
        if (result != null && Files.exists(result)) {
            logger.debug("Plugin not found in pluginInstall.coordinate.url, BUT found in killbill public repository. Downloaded Path: {}", result);
            return new DownloadResult(result, actualGroupId, actualArtifactId, actualPluginVersion);
        }

        final String msg = String.format("Cannot find plugin with key: %s, kbVersion: %s, groupId: %s, artifactId: %s, version: %s. " +
                                         "Note that at this point, groupId/artifactId/version might different with input due to search " +
                                         "operation by artifactAndVersionFinder.findArtifactAndVersion()",
                                         pluginKey, killbillVersion, actualGroupId, actualArtifactId, actualPluginVersion);

        throw new KPMPluginException(msg);
    }

    private Path doDownloadValidateAndVerify(final String pluginKey, final String version, final String pluginJarUrl, final String sha1Url) {
        try {
            final String fileName = PluginNamingResolver.of(pluginKey, version).getPluginJarFileName();
            final Path downloadedFile = Files.createTempDirectory("kpm").resolve(fileName);
            httpClient.download(pluginJarUrl, uriResolver.getHeaders(), downloadedFile);

            if (!Files.exists(downloadedFile) && Files.size(downloadedFile) <= 0L) {
                throw new KPMPluginException(String.format("Unable to download file named as '%s' from %s", fileName, pluginKey));
            }
            if (!isValidJarFile(downloadedFile)) {
                throw new KPMPluginException("Plugin downloaded successfully, but it is not a JAR file");
            } else {
                logger.debug("Downloaded plugin file is a valid JAR file");
            }
            if (!sha1Checker.isDownloadedFileVerified(sha1Url, uriResolver.getHeaders(), downloadedFile)) {
                throw new KPMPluginException("Plugin downloaded successfully, but SHA1 verification failed");
            } else {
                logger.debug("Downloaded plugin file is verified");
            }
            return downloadedFile;
        } catch (final IOException e) {
            throw new KPMPluginException("Problem when creating temporary file for download plugin", e);
        } catch (final Exception e) {
            logger.error("There's problem when downloading plugin from: {}. Exception: {}", pluginJarUrl, e.getMessage());
            return null;
        }
    }

    private Path downloadFromPluginInstallUrl(final String pluginKey, final String groupId, final String artifactId, final String version) {
        final String jarUrl = uriResolver.getBaseUri() + "/" + coordinateToUri(groupId, artifactId, version, ".jar");
        final String sha1Url = uriResolver.getBaseUri() + "/" + coordinateToUri(groupId, artifactId, version, ".jar.sha1");
        logger.debug("#downloadFromPluginInstallUrl() . jarUrl: {}, sha1Url: {}", jarUrl, sha1Url);

        return doDownloadValidateAndVerify(pluginKey, version, jarUrl, sha1Url);
    }

    private Path downloadFromPublicKillbill(final String pluginKey, final String groupId, final String artifactId, final String version) {
        // If downloadFromNexusUri() already from sonatype/maven, then do not repeat.
        // If shouldTryPublicRepository = false, then do not download.
        if (uriResolver.getBaseUri().startsWith("https://oss.sonatype.org") ||
            uriResolver.getBaseUri().startsWith("https://repo1.maven.org/maven2") ||
            !this.shouldTryPublicRepository) {
            return null;
        }

        final String jarUrl =  SONATYPE_RELEASE_URL + coordinateToUri(groupId, artifactId, version, ".jar");
        final String sha1Url = SONATYPE_RELEASE_URL + coordinateToUri(groupId, artifactId, version, ".jar.sha1");
        logger.debug("#downloadFromPublicKillbill() . jarUrl: {}, sha1Url: {}", jarUrl, sha1Url);

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
