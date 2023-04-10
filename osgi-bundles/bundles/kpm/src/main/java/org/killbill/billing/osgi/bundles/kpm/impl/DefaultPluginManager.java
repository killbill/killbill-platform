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
import java.util.Set;

import javax.annotation.Nonnull;

import org.killbill.billing.osgi.api.PluginStateChange;
import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.killbill.billing.osgi.bundles.kpm.PluginFileService;
import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.PluginIdentifiersDAO;
import org.killbill.billing.osgi.bundles.kpm.PluginInstaller;
import org.killbill.billing.osgi.bundles.kpm.PluginManager;
import org.killbill.billing.osgi.bundles.kpm.PluginsDirectoryDAO.PluginsDirectoryModel;
import org.killbill.billing.osgi.bundles.kpm.VersionsProvider;
import org.killbill.billing.osgi.bundles.kpm.impl.CoordinateBasedPluginDownloader.DownloadResult;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;

import org.killbill.commons.utils.annotation.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultPluginManager implements PluginManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPluginManager.class);

    private final OSGIKillbillAPI killbillApi;
    private final KPMClient httpClient;
    private final PluginFileService pluginFileService;
    private final PluginIdentifiersDAO pluginIdentifiersDAO;
    private final AvailablePluginsComponentsFactory availablePluginsComponentsFactory;
    private final CoordinateBasedPluginDownloader pluginDownloader;
    private final String adminUsername;
    private final String adminPassword;

    public DefaultPluginManager(@Nonnull final OSGIKillbillAPI killbillApi, @Nonnull final Properties properties) {
        this.killbillApi = killbillApi;
        this.pluginFileService = createPluginFileService(properties);
        this.pluginIdentifiersDAO = createPluginIdentifiersDAO(properties);
        this.httpClient = createHttpClient(properties);
        this.availablePluginsComponentsFactory = new AvailablePluginsComponentsFactory(killbillApi, httpClient, properties);
        this.pluginDownloader = createPluginDownloader(properties);

        this.adminUsername = Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "adminUsername"), "admin");
        this.adminPassword = Objects.requireNonNullElse(properties.getProperty(PROPERTY_PREFIX + "adminPassword"), "password");
    }

    @VisibleForTesting
    PluginFileService createPluginFileService(final Properties properties) {
        return new DefaultPluginFileService(properties);
    }

    @VisibleForTesting
    PluginIdentifiersDAO createPluginIdentifiersDAO(final Properties properties) {
        return new FileBasedPluginIdentifiersDAO(properties);
    }

    @VisibleForTesting
    KPMClient createHttpClient(final Properties properties) {
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

    CoordinateBasedPluginDownloader createPluginDownloader(final Properties properties) {
        final ArtifactAndVersionFinder finder = new ArtifactAndVersionFinder(pluginIdentifiersDAO, availablePluginsComponentsFactory);
        return new CoordinateBasedPluginDownloader(httpClient, finder, properties);
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
    public GetAvailablePluginsModel getAvailablePlugins(@Nonnull final String kbVersion,
                                                        final boolean forceDownload) throws KPMPluginException {
        final GetAvailablePluginsModel result = new GetAvailablePluginsModel();

        final VersionsProvider versionsProvider = availablePluginsComponentsFactory.createVersionsProvider(kbVersion, forceDownload);
        result.addKillbillVersion(versionsProvider.getFixedKillbillVersion());
        result.addOssParentVersion(versionsProvider.getOssParentVersion());
        result.addApiVersion(versionsProvider.getKillbillApiVersion());
        result.addPluginApiVersion(versionsProvider.getKillbillPluginApiVersion());
        result.addCommonsVersion(versionsProvider.getKillbillCommonsVersion());
        result.addPlatformVersion(versionsProvider.getKillbillPlatformVersion());

        final Set<PluginsDirectoryModel> plugins = availablePluginsComponentsFactory
                .createPluginsDirectoryDAO(versionsProvider.getFixedKillbillVersion(), forceDownload)
                .getPlugins();
        plugins.forEach(entry -> result.addPlugins(entry.getPluginKey(), entry.getPluginVersion()));

        return result;
    }

    @Override
    public void install(@Nonnull final String uri,
                        @Nonnull final String pluginKey,
                        @Nonnull final String pluginVersion) throws KPMPluginException {
        logger.debug("Installing plugin via URL for key: {}, pluginVersion: {}, uri: {}", pluginKey, pluginVersion, uri);

        Path downloadedFile = null;
        final PluginNamingResolver namingResolver = PluginNamingResolver.of(pluginKey, pluginVersion, uri);
        try {
            // Prepare temp file as download location
            final Path downloadDirectory = PluginFileService.createTmpDownloadPath();
            final String pluginFileName = namingResolver.getPluginJarFileName();
            final String fixedVersion = namingResolver.getPluginVersion();
            downloadedFile = downloadDirectory.resolve(pluginFileName);
            // Download
            httpClient.downloadPlugin(uri, downloadedFile);

            // install
            final PluginInstaller pluginInstaller = new URIBasedPluginInstaller(pluginFileService, downloadedFile, pluginKey, fixedVersion);
            pluginInstaller.install();

            // Add/update plugin identifier
            pluginIdentifiersDAO.add(pluginKey, fixedVersion);

            notifyFileSystemChange(PluginStateChange.NEW_VERSION, pluginKey, fixedVersion);

        } catch (final Exception e) {
            logger.error("Error when install plugin with URI:{}, key:{}, version:{}", uri, pluginKey, pluginVersion);
            throw new KPMPluginException(e);
        } finally {
            FilesUtils.deleteIfExists(downloadedFile);
        }
    }

    @Override
    public void install(@Nonnull final String pluginKey,
                        @Nonnull final String kbVersion,
                        String groupId,
                        String artifactId,
                        String pluginVersion,
                        final boolean forceDownload) throws KPMPluginException {
        logger.info("Install plugin via coordinate. key:{}, kbVersion:{}, version:{}, groupId:{}, artifactId:{}", pluginKey, kbVersion, pluginVersion, groupId, artifactId);

        DownloadResult downloadResult = null;
        Path installedPath = null;
        try {
            downloadResult = pluginDownloader.download(pluginKey, kbVersion, groupId, artifactId, pluginVersion, forceDownload);
            logger.debug("downloadResult object value: {}", downloadResult);

            // Update these values. Needed because these values are getting up-to-date during pluginDownloader.download(),
            // depends on which implementation used by artifactAndVersionFinder.findArtifactAndVersion(). See more
            // artifactAndVersionFinder.findArtifactAndVersion javadoc
            final Path downloadedPath = downloadResult.getDownloadedPath();
            groupId = downloadResult.getGroupId();
            artifactId = downloadResult.getArtifactId();
            pluginVersion = downloadResult.getPluginVersion();
            // FIXME TS-93:
            //   1. What happened if, just like github, we need username:password in URL?
            //   2. What happened if, cloudsmith need authentication info in header or body?
            final PluginInstaller pluginInstaller = new URIBasedPluginInstaller(pluginFileService, downloadedPath, pluginKey, pluginVersion);
            installedPath = pluginInstaller.install();

            // Add/update plugin identifier
            pluginIdentifiersDAO.add(pluginKey, groupId, artifactId, pluginVersion);

            notifyFileSystemChange(PluginStateChange.NEW_VERSION, pluginKey, pluginVersion);
            logger.info("Plugin key: {} installed successfully via coordinate.", pluginKey);
        } catch (final Exception e) {
            logger.error("Error when install pluginKey: '{}' with coordinate: Exception: {}", pluginKey, e);
            // If exception happened, installed file should be deleted.
            FilesUtils.deleteIfExists(installedPath);
            throw new KPMPluginException(e);
        } finally {
            if (downloadResult != null) {
                // FIXME-TS-93 : Check why this file not getting deleted
                FilesUtils.deleteIfExists(downloadResult.getDownloadedPath());
            }
        }
    }

    @Override
    public void uninstall(final String pluginKey, final String version) throws KPMPluginException {

    }
}
