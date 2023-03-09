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
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.Properties;
import java.util.SortedSet;

import org.killbill.billing.osgi.api.PluginStateChange;
import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.killbill.billing.osgi.bundles.kpm.KPMPluginOperationException;
import org.killbill.billing.osgi.bundles.kpm.PluginFileService;
import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.PluginIdentifierService;
import org.killbill.billing.osgi.bundles.kpm.PluginInstaller;
import org.killbill.billing.osgi.bundles.kpm.PluginManager;
import org.killbill.billing.osgi.bundles.kpm.PluginIdentifier;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultPluginManager implements PluginManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPluginManager.class);

    private final OSGIKillbillAPI killbillApi;
    private final KPMClient httpClient;
    private final PluginFileService pluginFileService;
    private final PluginIdentifierService pluginIdentifierService;
    private final String adminUsername;
    private final String adminPassword;

    public DefaultPluginManager(final OSGIKillbillAPI killbillApi,
                                final PluginFileService pluginFileService,
                                final PluginIdentifierService pluginIdentifierService,
                                final Properties properties) {
        this.killbillApi = killbillApi;
        this.pluginFileService = pluginFileService;
        this.pluginIdentifierService = pluginIdentifierService;
        this.httpClient = createHttpClient(properties);

        this.adminUsername = Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "adminUsername"), "admin");
        this.adminPassword = Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "adminPassword"), "password");
    }

    private KPMClient createHttpClient(final Properties properties) {
        final boolean strictSSL = Boolean.parseBoolean(Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "strictSSL"), "true"));
        final int connectTimeOutMs = Integer.parseInt(Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "connectTimeoutSec"), "60")) * 1000;
        final int readTimeOutMs = Integer.parseInt(Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "readTimeoutSec"), "60")) * 1000;

        try {
            // If exceptions are thrown here, the plugin cannot work properly in the first place
            return new KPMClient(strictSSL, connectTimeOutMs, readTimeOutMs);
        } catch (final GeneralSecurityException e) {
            throw new KPMPluginException("Cannot create KpmHttpClient, there's problem with SSL context creation.", e);
        }
    }

    private void notifyFileSystemChange(final PluginStateChange newState,
                                        final String pluginKey,
                                        final String pluginVersion) {
        try {
            logger.info("Notifying Kill Bill: state='{}', pluginKey='{}', pluginVersion={}", newState, pluginKey, pluginVersion);
            killbillApi.getSecurityApi().login(adminUsername, adminPassword);
            killbillApi.getPluginsInfoApi().notifyOfStateChanged(newState,
                                                                 pluginKey,
                                                                 null, // Not needed
                                                                 pluginVersion,
                                                                 null /* Unused */);
        } finally {
            killbillApi.getSecurityApi().logout();
        }
    }

    @Override
    public SortedSet<PluginIdentifier> getAvailablePlugins(final String kbVersion, final boolean forceDownload) throws KPMPluginException {
        return null;
    }

    @Override
    public void install(final String uri, final String pluginKey, final String pluginVersion) throws KPMPluginException {
        final PluginNamingResolver namingResolver = PluginNamingResolver.of(pluginKey, pluginVersion, uri);
        try {
            // Prepare temp file as download location
            final Path downloadDirectory = PluginFileService.createTmpDownloadPath();
            final String pluginFileName = namingResolver.getPluginJarFileName();
            final String fixedVersion = namingResolver.getPluginVersion();
            final Path downloadedFile = downloadDirectory.resolve(pluginFileName);
            // Download
            httpClient.downloadPlugin(uri, downloadedFile);

            // install
            final PluginInstaller pluginInstaller = new URIBasedPluginInstaller(pluginFileService, downloadedFile, pluginKey, fixedVersion);
            pluginInstaller.install();

            notifyFileSystemChange(PluginStateChange.NEW_VERSION, pluginKey, fixedVersion);

            // Add/update plugin identifier
            pluginIdentifierService.add(pluginKey, fixedVersion);

        } catch (final Exception e) {
            throw KPMPluginOperationException.newInstallException(e);
        }
    }

    @Override
    public void install(final String pluginKey,
                        final String killbillVersion,
                        final String pluginGroupId,
                        final String pluginArtifactId,
                        final String pluginVersion,
                        final String pluginClassifier,
                        final boolean forceDownload) throws KPMPluginException {

    }

    @Override
    public void uninstall(final String pluginKey, final String version) throws KPMPluginException {

    }
}
