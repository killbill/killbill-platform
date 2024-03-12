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

    private static final String PLUGINS_DIRECTORY_PREFIX = PROPERTY_PREFIX + "pluginsDirectory.";

    private static final String PLUGIN_INSTALL_PREFIX = PROPERTY_PREFIX + "pluginInstall.";

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
     * @return get {@code org.killbill.billing.plugin.kpm.adminUsername} property, or {@code admin} if not set.
     */
    public String getKillbillAdminUsername() {
        return Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "adminUsername"), "admin");
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.adminPassword} property, or {@code password} if not set.
     */
    public String getKillbillAdminPassword() {
        return Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "adminPassword"), "password");
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.nexusUrl} property, or {@code https://oss.sonatype.org/} if not set.
     */
    public String getNexusUrl() {
        return Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "nexusUrl"), "https://oss.sonatype.org");
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.nexusRepository} property, or {@code releases} if not set.
     */
    public String getNexusRepository() {
        return Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "nexusRepository"), "/releases");
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.nexusMavenMetadataUrl} property, or
     *         {@code https://repo1.maven.org/maven2/org/kill-bill/billing/killbill/maven-metadata.xml}.
     */
    public String getNexusMavenMetadataUrl() {
        return Objects.requireNonNullElse(
                properties.getProperty(PROPERTY_PREFIX + "nexusMavenMetadataUrl"),
                "https://repo1.maven.org/maven2/org/kill-bill/billing/killbill/maven-metadata.xml");
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.nexusAuthMethod} property, or {@code NONE} if not set.
     */
    public String getNexusAuthMethod() {
        return Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "nexusAuthMethod"), "NONE");
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.nexusAuthUsername} property, or string {@code username} if not set.
     */
    public String getNexusAuthUsername() {
        return Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "nexusAuthUsername"), "username");
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.nexusAuthPassword} property, or string {@code password} if not set.
     */
    public String getNexusAuthPassword() {
        return Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "nexusAuthPassword"), "password");
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.nexusAuthToken} property, or string {@code VALID_TOKEN} if not set.
     */
    public String getNexusAuthToken() {
        return Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "nexusAuthToken"), "VALID_TOKEN");
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
        return Integer.parseInt(Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "readTimeoutSec"), "60"));
    }

    /**
     * @return get {@code org.killbill.billing.plugin.kpm.connectTimeoutSec} property, or {@code 60} if not set.
     */
    public int getConnectTimeoutSec() {
        return Integer.parseInt(Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "connectTimeoutSec"), "60"));
    }

    /** Navigate to {@link AvailablePlugins} property path */
    public AvailablePlugins availablePlugins() {
        return new AvailablePlugins();
    }

    /** Navigate to {@link PluginsDirectory} property path */
    public PluginsDirectory pluginsDirectory() {
        return new PluginsDirectory();
    }

    public PluginsInstall pluginsInstall() {
        return new PluginsInstall();
    }

    // -- AvailablePlugins
    public final class AvailablePlugins {

        public Cache cache() {
            return new Cache();
        }

        // -- AvailablePlugins.Cache
        public final class Cache {

            /**
             * @return get {@code org.killbill.billing.plugin.kpm.availablePlugins.cache.size} property, or {@code 10} if not set.
             */
            public int getSize() {
                return Integer.parseInt(Objects.requireNonNullElse(properties.getProperty(AVAILABLE_PLUGINS_PREFIX + "cache.size"), "10"));
            }

            /**
             * @return get {@code org.killbill.billing.plugin.kpm.availablePlugins.cache.expirationSecs} or {@code 86400 (24 hours)} if not set
             */
            public int getExpirationSec() {
                return Integer.parseInt(Objects.requireNonNullElse(properties.getProperty(AVAILABLE_PLUGINS_PREFIX + "cache.expirationSecs"), "86460"));
            }

            /**
             * @return get {@code org.killbill.billing.plugin.kpm.availablePlugins.cache.enabled} or {@code false} it not set.
             */
            public boolean isEnabled() {
                return Boolean.parseBoolean(properties.getProperty(AVAILABLE_PLUGINS_PREFIX + "cache.enabled"));
            }
        }
    }

    // -- PluginsDirectory
    public final class PluginsDirectory {

        /**
         * @return get {@code org.killbill.billing.plugin.kpm.pluginsDirectory.url} property, or
         *         {@code https://raw.githubusercontent.com/killbill/killbill-cloud/master/kpm/lib/kpm/plugins_directory.yml}.
         */
        public String getUrl() {
            return Objects.requireNonNullElse(
                    properties.getProperty(PLUGINS_DIRECTORY_PREFIX + "url"),
                    "https://raw.githubusercontent.com/killbill/killbill-cloud/master/kpm/lib/kpm/plugins_directory.yml");
        }

        /**
         * @return get {@code org.killbill.billing.plugin.kpm.pluginsDirectory.authMethod} property, or {@code NONE}.
         */
        public String getAuthMethod() {
            return Objects.requireNonNullElse(properties.getProperty(PLUGINS_DIRECTORY_PREFIX + "authMethod"), "NONE");
        }

        /**
         * @return get {@code org.killbill.billing.plugin.kpm.pluginsDirectory.authUsername} property, or {@code <none>}.
         */
        public String getAuthUsername() {
            return Objects.requireNonNullElse(properties.getProperty(PLUGINS_DIRECTORY_PREFIX + "authUsername"), "<none>");
        }

        /**
         * @return get {@code org.killbill.billing.plugin.kpm.pluginsDirectory.authPassword} property, or {@code <none>}.
         */
        public String getAuthPassword() {
            return Objects.requireNonNullElse(properties.getProperty(PLUGINS_DIRECTORY_PREFIX + "authPassword"), "<none>");
        }

        /**
         * @return get {@code org.killbill.billing.plugin.kpm.pluginsDirectory.authToken} property, or {@code <none>}.
         */
        public String getAuthToken() {
            return Objects.requireNonNullElse(properties.getProperty(PLUGINS_DIRECTORY_PREFIX + "authToken"), "<none>");
        }
    }

    // -- PluginsInstall
    public final class PluginsInstall {

        public Coordinate coordinate() {
            return new Coordinate();
        }

        // -- PluginsInstall.Coordinate
        public final class Coordinate {

            /**
             * @return get {@code org.killbill.billing.plugin.kpm.pluginInstall.coordinate.isVerifySHA1}, or {@code false}.
             */
            public boolean isVerifySHA1() {
                return Boolean.parseBoolean(properties.getProperty(PLUGIN_INSTALL_PREFIX + "coordinate.isVerifySHA1"));
            }

            /**
             * @return get {@code org.killbill.billing.plugin.kpm.pluginsInstall.coordinate.url} property, or
             *         {@link #getNexusUrl()} + {@link #getNexusRepository()}.
             */
            public String getUrl() {
                return Objects.requireNonNullElse(
                        properties.getProperty(PLUGIN_INSTALL_PREFIX + "coordinate.url"),
                        getNexusUrl() + "/" + getNexusRepository());
            }

            /**
             * @return get {@code org.killbill.billing.plugin.kpm.pluginsInstall.coordinate.authMethod} property, or {@link #getNexusAuthMethod()}.
             */
            public String getAuthMethod() {
                return Objects.requireNonNullElse(properties.getProperty(PLUGIN_INSTALL_PREFIX + "coordinate.authMethod"), getNexusAuthMethod());
            }

            /**
             * @return get {@code org.killbill.billing.plugin.kpm.pluginsInstall.coordinate.authUsername} property, or {@link #getNexusAuthUsername()}.
             */
            public String getAuthUsername() {
                return Objects.requireNonNullElse(properties.getProperty(PLUGIN_INSTALL_PREFIX + "coordinate.authUsername"), getNexusAuthUsername());
            }

            /**
             * @return get {@code org.killbill.billing.plugin.kpm.pluginsInstall.coordinate.authPassword} property, or {@link #getNexusAuthPassword()}.
             */
            public String getAuthPassword() {
                return Objects.requireNonNullElse(properties.getProperty(PLUGIN_INSTALL_PREFIX + "coordinate.authPassword"), getNexusAuthPassword());
            }

            /**
             * @return get {@code org.killbill.billing.plugin.kpm.pluginsInstall.coordinate.authToken} property, or {@link #getNexusAuthToken()}
             */
            public String getAuthToken() {
                return Objects.requireNonNullElse(properties.getProperty(PLUGIN_INSTALL_PREFIX + "coordinate.authToken"), getNexusAuthToken());
            }

            /**
             * @return get {@code org.killbill.billing.plugin.kpm.pluginsInstall.coordinate.alwaysTryPublicRepository} property, or {@code false}.
             */
            public boolean isAlwaysTryPublicRepository() {
                return Boolean.parseBoolean(properties.getProperty(PLUGIN_INSTALL_PREFIX + "coordinate.alwaysTryPublicRepository"));
            }
        }
    }
}
