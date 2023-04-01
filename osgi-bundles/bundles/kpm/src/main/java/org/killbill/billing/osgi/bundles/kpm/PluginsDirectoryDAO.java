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

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 *<p>
 *     Provide a way to accessing plugin directory information. Currently, the only plugin directory available is in
 *     <a href="https://raw.githubusercontent.com/killbill/killbill-cloud/master/kpm/lib/kpm/plugins_directory.yml">GitHub repository</a>.
 * </p>
 *
 * <p>See also {@code org.killbill.billing.osgi.bundles.kpm.impl.DefaultPluginsDirectoryDAO}</p>
 */
public interface PluginsDirectoryDAO {

    Set<PluginsDirectoryModel> getPlugins();

    String DEFAULT_DIRECTORY = "https://raw.githubusercontent.com/killbill/killbill-cloud/master/kpm/lib/kpm/plugins_directory.yml";

    PluginsDirectoryDAO NONE = Collections::emptySet;

    /**
     * In this context, pluginKey and pluginVersion is the only things that is mandatory.
     */
    class PluginsDirectoryModel implements Comparable<PluginsDirectoryModel> {

        private final String pluginKey;
        private final String pluginVersion;

        // Optional, and added to support ArtifactAndVersionFinder#findArtifactAndVersion()
        private String pluginArtifactId;

        public PluginsDirectoryModel(final String pluginKey, final String pluginVersion) {
            this.pluginKey = pluginKey;
            this.pluginVersion = pluginVersion;
        }

        public String getPluginKey() {
            return pluginKey;
        }

        public String getPluginVersion() {
            return pluginVersion;
        }

        public String getPluginArtifactId() {
            return pluginArtifactId;
        }

        public void setPluginArtifactId(final String pluginArtifactId) {
            this.pluginArtifactId = pluginArtifactId;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final PluginsDirectoryModel that = (PluginsDirectoryModel) o;
            return pluginKey.equals(that.pluginKey) && pluginVersion.equals(that.pluginVersion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pluginKey, pluginVersion);
        }

        @Override
        public int compareTo(final PluginsDirectoryModel o) {
            return pluginKey.compareTo(o.getPluginKey());
        }

        @Override
        public String toString() {
            return "AvailablePluginsModel{" +
                   "pluginKey='" + pluginKey + '\'' +
                   ", pluginVersion='" + pluginVersion + '\'' +
                   ", pluginArtifactId='" + pluginArtifactId + '\'' +
                   '}';
        }
    }
}
