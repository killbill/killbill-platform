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

import org.killbill.billing.osgi.bundles.kpm.KpmProperties;
import org.killbill.billing.osgi.bundles.kpm.KpmProperties.PluginsDirectory;
import org.killbill.billing.osgi.bundles.kpm.KpmProperties.PluginsInstall.Coordinate;
import org.killbill.billing.osgi.bundles.kpm.UriResolver;
import org.killbill.billing.osgi.bundles.kpm.UriResolver.AuthenticationMethod;

public class UrlResolverFactory {

    private final KpmProperties kpmProperties;

    public UrlResolverFactory(final KpmProperties kpmProperties) {
        this.kpmProperties = kpmProperties;
    }

    // This is for backward compatibility: by default, nexusUrl is "https://oss.sonatype.org/" and
    // nexusRepository is "releases". But if we just simply concat them like 'https://oss.sonatype.org/releases',
    // this is not valid sonatype URL.
    // However, this is only applied to 'NONE' authentication method, because for other authentication methods, it is
    // very likely that users have another url other than 'https://oss.sonatype.org/' and 'releases'. We can't guess and
    // add something like "/content/repositories" at this point.
    private String getValidUrlIfSonatype(final String url) {
        return url.contains("oss.sonatype.org") ?
               kpmProperties.getNexusUrl().concat("/content/repositories").concat(kpmProperties.getNexusRepository()) :
               url;
    }

    /**
     * Will create {@link UriResolver} instance for configuration:
     * <ul>
     *     <li>org.killbill.billing.plugin.kpm.nexusUrl</li>
     *     <li>org.killbill.billing.plugin.kpm.nexusRepository</li>
     * </ul>
     * Used mainly by {@link org.killbill.billing.osgi.bundles.kpm.NexusMetadataFiles}. See also
     * {@link org.killbill.billing.osgi.bundles.kpm.impl.AvailablePluginsComponentsFactory#createVersionsProvider(String, boolean)}
     */
    public UriResolver getVersionsProviderUrlResolver() {
        final AuthenticationMethod authMethod = AuthenticationMethod.valueOf(kpmProperties.getNexusAuthMethod().toUpperCase());
        switch (authMethod) {
            case NONE:
                String baseUrl = getValidUrlIfSonatype(kpmProperties.getNexusUrl() + kpmProperties.getNexusRepository());
                return new NoneUriResolver(baseUrl);

            case BASIC:
                baseUrl = kpmProperties.getNexusUrl().concat(kpmProperties.getNexusRepository());
                final String username = kpmProperties.getNexusAuthUsername();
                final String password = kpmProperties.getNexusAuthPassword();
                return new BasicUriResolver(baseUrl, username, password);

            case TOKEN:
                baseUrl = kpmProperties.getNexusUrl().concat(kpmProperties.getNexusRepository());
                return new TokenUriResolver(baseUrl, kpmProperties.getNexusAuthToken());

            default: throw new IllegalStateException("Unknown authentication method: " + kpmProperties.getNexusAuthMethod());
        }
    }

    /**
     * Will create {@link UriResolver} instance for configuration: {@code org.killbill.billing.plugin.kpm.availablePlugins},
     * mainly by {@link org.killbill.billing.osgi.bundles.kpm.PluginsDirectoryDAO}. See also
     * {@link org.killbill.billing.osgi.bundles.kpm.impl.AvailablePluginsComponentsFactory#createPluginsDirectoryDAO(String, boolean)}.
     */
    public UriResolver getPluginDirectoryUrlResolver() {
        final PluginsDirectory pluginsDirectory = kpmProperties.pluginsDirectory();
        final AuthenticationMethod authMethod = AuthenticationMethod.valueOf(pluginsDirectory.getAuthMethod());
        switch (authMethod) {
            case NONE:
                return new NoneUriResolver(getValidUrlIfSonatype(pluginsDirectory.getUrl()));

            case BASIC:
                return new BasicUriResolver(pluginsDirectory.getUrl(), pluginsDirectory.getAuthUsername(), pluginsDirectory.getAuthPassword());

            case TOKEN:
                return new TokenUriResolver(pluginsDirectory.getUrl(), pluginsDirectory.getAuthToken());

            default:
                throw new IllegalStateException("Unknown authentication method: " + pluginsDirectory.getAuthMethod());
        }
    }

    /**
     * Will create {@link UriResolver} instance for configuration: {@code org.killbill.billing.plugin.kpm.pluginInstall.*}.
     * Mainly used by {@link org.killbill.billing.osgi.bundles.kpm.impl.CoordinateBasedPluginDownloader}. See also
     * {@link DefaultPluginManager#createCoordinateBasedPluginDownloader(KpmProperties)}.
     */
    public UriResolver getCoordinateBasedPluginDownloaderUrlResolver() {
        final Coordinate pluginsInstallCoordinate = kpmProperties.pluginsInstall().coordinate();
        final AuthenticationMethod authMethod = AuthenticationMethod.valueOf(pluginsInstallCoordinate.getAuthMethod());
        switch (authMethod) {
            case NONE:
                return new NoneUriResolver(getValidUrlIfSonatype(pluginsInstallCoordinate.getUrl()));

            case BASIC:
                return new BasicUriResolver(pluginsInstallCoordinate.getUrl(),
                                            pluginsInstallCoordinate.getAuthUsername(),
                                            pluginsInstallCoordinate.getAuthPassword());
            case TOKEN:
                return new TokenUriResolver(pluginsInstallCoordinate.getUrl(), pluginsInstallCoordinate.getAuthToken());

            default:
                throw new IllegalStateException("Unknown authentication method: " + pluginsInstallCoordinate.getAuthMethod());
        }
    }
}
