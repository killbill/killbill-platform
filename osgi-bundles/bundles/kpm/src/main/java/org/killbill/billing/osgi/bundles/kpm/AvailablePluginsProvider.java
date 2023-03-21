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
 * Provide available plugins information from remote location.
 */
public interface AvailablePluginsProvider {

    Set<AvailablePluginsModel> getAvailablePlugins();

    AvailablePluginsProvider NONE = Collections::emptySet;

    class AvailablePluginsModel {

        private final String pluginKey;
        private final String pluginVersion;

        public AvailablePluginsModel(final String pluginKey, final String pluginVersion) {
            this.pluginKey = pluginKey;
            this.pluginVersion = pluginVersion;
        }

        public String getPluginKey() {
            return pluginKey;
        }

        public String getPluginVersion() {
            return pluginVersion;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final AvailablePluginsModel that = (AvailablePluginsModel) o;
            return pluginKey.equals(that.pluginKey) && pluginVersion.equals(that.pluginVersion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pluginKey, pluginVersion);
        }
    }
}
