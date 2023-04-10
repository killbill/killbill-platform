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

import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import org.killbill.commons.utils.Strings;

public final class KpmProperties {

    private static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.kpm.";

    private static final String AVAILABLE_PLUGINS_PREFIX = PROPERTY_PREFIX + "availablePlugins.";

    private final Properties properties = new Properties();

    public KpmProperties(final Properties killbillProperties) {
        // See comment on DefaultKillbillConfigSource#getProperties()
        killbillProperties.stringPropertyNames().forEach(key -> properties.setProperty(key, killbillProperties.getProperty(key)));
    }

    /**
     * @return get {@code org.killbill.osgi.bundle.install.dir} property value, or {@code /var/tmp/bundles} if not set.
     */
    public Path getBundlesPath() {
        final String bundleInstallDir = properties.getProperty("org.killbill.osgi.bundle.install.dir");
        return Strings.isNullOrEmpty(bundleInstallDir) ? Path.of("/var", "tmp", "bundles") : Path.of(bundleInstallDir);
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.nexusUrl} property, or {@code https://oss.sonatype.org} if not set.
     */
    public String getNexusUrl() {
        return Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "nexusUrl"), "https://oss.sonatype.org");
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.nexusRepository} property, or {@code releases} if not set.
     */
    public String getNexusRepository() {
        return Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "nexusRepository"), "releases");
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.strictSSL} property, or {@code false} if not set.
     */
    public boolean isStrictSSL() {
        return Boolean.parseBoolean(properties.getProperty(PROPERTY_PREFIX + "strictSSL"));
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.readTimeoutSec} property, or {@code 60} if not set.
     */
    public int getReadTimeoutSec() {
        return Objects.requireNonNullElse(Integer.valueOf(properties.getProperty(PROPERTY_PREFIX + "readTimeoutSec")), 60);
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.connectTimeoutSec} property, or {@code 60} if not set.
     */
    public int getConnectTimeoutSec() {
        return Objects.requireNonNullElse(Integer.valueOf(properties.getProperty(PROPERTY_PREFIX + "connectTimeoutSec")), 60);
    }

    /**
     * Navigate to {@code availablePlugins} specifics configuration.
     */
    public AvailablePlugins availablePlugins() {
        return new AvailablePlugins();
    }

    private class AvailablePlugins {

        /**
         * @return get {@code org.killbill.billing.plugin.kpm.availablePlugins.pluginsDirectoryUrl} config property, or
         * <a href="https://raw.githubusercontent.com/killbill/killbill-cloud/master/kpm/lib/kpm/plugins_directory.yml">this default</a>.
         */
        public String getPluginsDirectoryUrl() {
            return Objects.requireNonNullElse(
                    properties.getProperty(AVAILABLE_PLUGINS_PREFIX + "pluginsDirectoryUrl"),
                    "https://raw.githubusercontent.com/killbill/killbill-cloud/master/kpm/lib/kpm/plugins_directory.yml");
        }

        /**
         * Navigate to {@code availablePlugins.cache} configuration.
         */
        public Cache cache() {
            return new Cache();
        }

        private class Cache {

            /**
             * @return get {@code org.killbill.billing.plugin.kpm.availablePlugins.cache.size} property, or {@code 10} if not set.
             */
            public int getSize() {
                return Objects.requireNonNullElse(Integer.valueOf(properties.getProperty(AVAILABLE_PLUGINS_PREFIX + "cache.size")), 10);
            }

            /**
             * @return get {@code org.killbill.billing.plugin.kpm.availablePlugins.cache.expirationSecs} or {@code 86400 (24 hours)} if not set
             */
            public int getExpirationSec() {
                return Objects.requireNonNullElse(Integer.valueOf(properties.getProperty(AVAILABLE_PLUGINS_PREFIX + "cache.expirationSecs")), 86_460);
            }

            /**
             *
             * @return get {@code org.killbill.billing.plugin.kpm.availablePlugins.cache.bypass} or {@code false} it not set.
             */
            public boolean isBypass() {
                return Boolean.parseBoolean(properties.getProperty(AVAILABLE_PLUGINS_PREFIX + "cache.bypass"));
            }
        }
    }
}
