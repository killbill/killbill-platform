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

package org.killbill.billing.osgi.bundles.kpm;

import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import org.killbill.commons.utils.annotation.VisibleForTesting;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * <p>Define an operations to deal with plugin identifier. Currently, the only place to manage plugin identifier is a file
 * located in {@code properties.getProperty("org.killbill.osgi.bundle.install.dir")/plugins/plugin_identifiers.json}.</p>
 *
 * <p>See also {@code org.killbill.billing.osgi.bundles.kpm.impl.FileBasedPluginIdentifiersDAO}</p>
 */
public interface PluginIdentifiersDAO {

    void add(@Nonnull final String pluginKey, @Nonnull final String version);

    void add(@Nonnull final String pluginKey, @Nonnull final String groupId, @Nonnull final String artifactId, @Nonnull final String version);

    void remove(@Nonnull final String pluginKey);

    Set<PluginIdentifiersModel> getPluginIdentifiers();


    /**
     * Represent a "pair of {@link PluginIdentifierModel} and its pluginKey". This is a default structure of
     * "plugin_identifiers.json" file.
     */
    final class PluginIdentifiersModel {

        private final String pluginKey;
        private final PluginIdentifierModel pluginIdentifier;

        public PluginIdentifiersModel(final String pluginKey, final PluginIdentifierModel pluginIdentifier) {
            this.pluginKey = pluginKey;
            this.pluginIdentifier = pluginIdentifier;
        }

        public String getPluginKey() {
            return pluginKey;
        }

        public PluginIdentifierModel getPluginIdentifier() {
            return pluginIdentifier;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final PluginIdentifiersModel that = (PluginIdentifiersModel) o;
            return pluginKey.equals(that.pluginKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pluginKey);
        }

        @Override
        public String toString() {
            return "PluginIdentifiersModel{" +
                   "pluginKey='" + pluginKey + '\'' +
                   ", pluginIdentifier=" + pluginIdentifier +
                   '}';
        }
    }

    /**
     * Attributes for each key of {@link PluginIdentifiersModel}. Contains jackson annotation because this will be
     * serialized to file.
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    final class PluginIdentifierModel {

        private final String pluginName;
        private final String groupId;
        private final String artifactId;
        private final String packaging = "jar";
        private final String classifier;
        private final String version;
        private final String language = "java";

        private PluginIdentifierModel(final String pluginName,
                                      final String groupId,
                                      final String artifactId,
                                      final String classifier,
                                      final String version) {
            this.pluginName = pluginName;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.classifier = classifier;
            this.version = version;
        }

        public PluginIdentifierModel(final String pluginName, final String version) {
            this(pluginName, null, null, null, version);
        }

        public PluginIdentifierModel(final String pluginName, final String groupId, final String artifactId, final String version) {
            this(pluginName, groupId, artifactId, null, version);
        }

        // Required by jackson.
        @VisibleForTesting
        PluginIdentifierModel() {
            this(null, null, null, null, null);
        }

        public String getPluginName() {
            return pluginName;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getPackaging() {
            return packaging;
        }

        public String getClassifier() {
            return classifier;
        }

        public String getVersion() {
            return version;
        }

        public String getLanguage() {
            return language;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final PluginIdentifierModel that = (PluginIdentifierModel) o;
            return pluginName.equals(that.pluginName) &&
                   Objects.equals(groupId, that.groupId) &&
                   Objects.equals(artifactId, that.artifactId) &&
                   Objects.equals(classifier, that.classifier) &&
                   version.equals(that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pluginName, groupId, artifactId, classifier, version);
        }

        @Override
        public String toString() {
            return "PluginIdentifierModel{" +
                   "pluginName='" + pluginName + '\'' +
                   ", groupId='" + groupId + '\'' +
                   ", artifactId='" + artifactId + '\'' +
                   ", packaging='" + packaging + '\'' +
                   ", classifier='" + classifier + '\'' +
                   ", version='" + version + '\'' +
                   ", language='" + language + '\'' +
                   '}';
        }
    }
}
