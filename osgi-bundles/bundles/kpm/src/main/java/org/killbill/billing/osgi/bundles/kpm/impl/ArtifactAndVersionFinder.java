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

import java.util.Optional;

import org.killbill.billing.osgi.bundles.kpm.PluginIdentifiersDAO;
import org.killbill.billing.osgi.bundles.kpm.PluginIdentifiersDAO.PluginIdentifiersModel;
import org.killbill.billing.osgi.bundles.kpm.PluginsDirectoryDAO.PluginsDirectoryModel;
import org.killbill.commons.utils.Strings;
import org.killbill.commons.utils.annotation.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Used to get artifact and version value, since {@code pluginArtifactId} and or {@code pluginVersion} parameters in
 * {@code PluginDownloader#download()} receive a null/empty value.</p>
 *
 * <p>We can't just append {@code "-plugin"} using something like {@link PluginNamingResolver#getPluginName()} to every
 * pluginKey because some plugin have different naming convention (for example, email notification has
 * {@code pluginKey="email-notification"}, where its {@code artifactId="killbill-email-notifications-plugin"}).</p>
 *
 * @see #findArtifactAndVersion(String, String, String, String, boolean) findArtifactAndVersion javadocs for detail.
 */
class ArtifactAndVersionFinder {

    private final Logger logger = LoggerFactory.getLogger(ArtifactAndVersionFinder.class);
    private final PluginIdentifiersDAO pluginIdentifiersDAO;
    private final AvailablePluginsComponentsFactory availablePluginsComponentsFactory;

    ArtifactAndVersionFinder(final PluginIdentifiersDAO pluginIdentifiersDAO, final AvailablePluginsComponentsFactory factory) {
        this.pluginIdentifiersDAO = pluginIdentifiersDAO;
        this.availablePluginsComponentsFactory = factory;
    }

    /**
     * <p>
     *     This method trust the input. Thus, if {@code pluginArtifactId} and {@code pluginVersion} is not null/empty,
     *     it will return.
     * </p>
     * <p>
     *     If {@code forceDownload=false}, this method will search in {@code PluginIdentifiersDAO} first, by calling
     *     {@link PluginIdentifiersDAO#getPluginIdentifiers()} method, and filter by {@code pluginKey}. If all
     *     {@code pluginArtifactId} and {@code pluginVersion} found in this phrase, this method will return.
     * </p>
     * <p>
     *     If {@code PluginIdentifiersDAO#getPluginIdentifiers()} still not set artifactId and version, this method
     *     will try to get info from {@code plugins_directory.yml} using {@code PluginsDirectoryDAO} by calling
     *     {@link AvailablePluginsComponentsFactory#createPluginsDirectoryDAO(String, boolean)}.
     * </p>
     * <p>
     *     As last attempt, if version is found but artifactId still null/empty, this method will set artifactId with
     *     {@code PluginNamingResolver.of(pluginKey).getPluginName()}.
     * </p>
     * <p>This method will return {@link Optional#empty()} if none of above method having a result</p>
     */
    Optional<ArtifactAndVersionModel> findArtifactAndVersion(final String killbillVersion,
                                                             final String pluginKey,
                                                             final String pluginArtifactId,
                                                             final String pluginVersion,
                                                             final boolean forceDownload) {
        logger.info("#findArtifactAndVersion() kbVersion: {}, pluginKey: {}, artifactId: {}, pluginVersion: {}, forceDownload: {}",
                    killbillVersion, pluginKey, pluginArtifactId, pluginVersion, forceDownload);

        // Trust the input. We only search if input is not found.
        if (!Strings.isNullOrEmpty(pluginArtifactId) && !Strings.isNullOrEmpty(pluginVersion)) {
            return Optional.of(new ArtifactAndVersionModel(pluginArtifactId, pluginVersion));
        }

        ArtifactAndVersionModel result = this.searchFromPluginsIdentifier(pluginKey, forceDownload);
        if (result.isAttributesSet()) {
            return Optional.of(result);
        }

        result = this.searchFromPluginsDirectory(killbillVersion, pluginKey, forceDownload);
        if (result.isAttributesSet()) {
            return Optional.of(result);
        }

        // If only artifactId that still null/empty, then use simple PluginNamingResolver approach as last attempt guess.
        if (!Strings.isNullOrEmpty(result.getVersion()) && Strings.isNullOrEmpty(result.getArtifactId())) {
            final String pluginNameAsArtifactId = PluginNamingResolver.of(pluginKey).getPluginName();
            result.setArtifactIdIfNull(pluginNameAsArtifactId);

            logger.debug("#findArtifactAndVersion() will set artifactId='{}' using PluginNamingResolver", pluginNameAsArtifactId);

            return Optional.of(result);
        }

        logger.info("#findArtifactAndVersion() will return empty object. Cannot find plugin key: '{}' in PluginIdentifiers and PluginsDirectory", pluginKey);
        return Optional.empty();
    }

    /**
     * Search artifact and version information using {@code PluginsIdentifiersDAO}.
     *
     * @param pluginKey pluginKey to search in {@code plugin_identifiers.json} file.
     * @param forceDownload if {@code true}, then this method will be skipped and use
     *                      {@link #searchFromPluginsDirectory(String, String, boolean)} instead.
     * @return ArtifactAndVersionModel instance.
     */
    @VisibleForTesting
    ArtifactAndVersionModel searchFromPluginsIdentifier(final String pluginKey, final boolean forceDownload) {
        final ArtifactAndVersionModel result = new ArtifactAndVersionModel();
        if (forceDownload) {
            logger.debug("#searchFromPluginsIdentifier() forceDownload=true. Search will be skipped and use #searchFromPluginsDirectory() instead");
            return result;
        }

        final PluginIdentifiersModel model = pluginIdentifiersDAO.getPluginIdentifiers().stream()
                                                                 .filter(m -> pluginKey.equals(m.getPluginKey()) && m.getPluginIdentifier() != null)
                                                                 .findFirst().orElse(null);

        if (model == null || model.getPluginIdentifier() == null) {
            logger.debug("#searchFromPluginsIdentifier() with pluginKey: '{}' does not have any result", pluginKey);
            return result;
        }

        logger.debug("#searchFromPluginsIdentifier() with pluginKey: '{}' found pluginIdentifier's artifact/version: {}", pluginKey, model);
        result.setArtifactIdIfNull(model.getPluginIdentifier().getArtifactId());
        result.setVersionIfNull(model.getPluginIdentifier().getVersion());
        if (result.isAttributesSet()) {
            logger.debug("#searchFromPluginsIdentifier() found all artifact and version attributes: {}", result);
        } else {
            logger.debug("#searchFromPluginsIdentifier() still have null/empty model attributes: {}", result);
        }
        return result;
    }

    @VisibleForTesting
    ArtifactAndVersionModel searchFromPluginsDirectory(final String killbillVersion, final String pluginKey, final boolean forceDownload) {
        logger.debug("#searchFromPluginsDirectory() using PluginsDirectoryDAO#createPluginsDirectoryDAO() using pluginKey: {}", pluginKey);
        final ArtifactAndVersionModel result = new ArtifactAndVersionModel();

        // Naturally, this method use availablePluginsComponentsFactory.createPluginsDirectoryDAO() to list plugins,
        // hence we need 'forceDownload' option. The 'forceDownload' behaviour may be truncated depends on
        // availablePluginsComponentsFactory.createPluginsDirectoryDAO() implementation.
        final PluginsDirectoryModel model = availablePluginsComponentsFactory
                .createPluginsDirectoryDAO(killbillVersion, forceDownload)
                .getPlugins()
                .stream()
                .filter(m -> m.getPluginKey().equals(pluginKey))
                .findFirst()
                .orElse(null);

        if (model != null) {
            logger.debug("#searchFromPluginsDirectory() found artifact/version from PluginsDirectoryDAO: {}", model);
            result.setArtifactIdIfNull(model.getPluginArtifactId());
            result.setVersionIfNull(model.getPluginVersion());
        }

        if (result.isAttributesSet()) {
            logger.debug("#searchFromPluginsDirectory() all ArtifactAndVersionModel attributes set : {}", result);
        } else {
            logger.debug("#searchFromPluginsDirectory() one or more ArtifactAndVersionModel attribute(s) still contains null/empty: {}", result);
        }

        return result;
    }

    static class ArtifactAndVersionModel {

        private String artifactId;
        private String version;

        ArtifactAndVersionModel() {
        }

        public ArtifactAndVersionModel(final String artifactId, final String version) {
            this.artifactId = artifactId;
            this.version = version;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactIdIfNull(final String artifactId) {
            this.artifactId = Strings.isNullOrEmpty(this.artifactId) ? artifactId : this.artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersionIfNull(final String version) {
            this.version = Strings.isNullOrEmpty(this.version) ? version : this.version;
        }

        public boolean isAttributesSet() {
            return !Strings.isNullOrEmpty(artifactId) && !Strings.isNullOrEmpty(version);
        }

        @Override
        public String toString() {
            return "ArtifactAndVersionModel{" +
                   "artifactId='" + artifactId + '\'' +
                   ", version='" + version + '\'' +
                   '}';
        }
    }
}
