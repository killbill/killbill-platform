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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Set;

import javax.annotation.Nonnull;

import org.killbill.billing.osgi.api.PluginStateChange;
import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.KpmProperties;
import org.killbill.billing.osgi.bundles.kpm.PluginFileService;
import org.killbill.billing.osgi.bundles.kpm.PluginIdentifiersDAO;
import org.killbill.billing.osgi.bundles.kpm.PluginInstaller;
import org.killbill.billing.osgi.bundles.kpm.PluginManager;
import org.killbill.billing.osgi.bundles.kpm.PluginsDirectoryDAO.PluginsDirectoryModel;
import org.killbill.billing.osgi.bundles.kpm.UriResolver;
import org.killbill.billing.osgi.bundles.kpm.VersionsProvider;
import org.killbill.billing.osgi.bundles.kpm.impl.CoordinateBasedPluginDownloader.DownloadResult;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.plugin.util.http.InvalidRequest;
import org.killbill.commons.utils.Strings;
import org.killbill.commons.utils.annotation.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultPluginManager implements PluginManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPluginManager.class);

    private final OSGIKillbillAPI killbillApi;
    private final KPMClient httpClient;
    private final PluginIdentifiersDAO pluginIdentifiersDAO;
    private final AvailablePluginsComponentsFactory availablePluginsComponentsFactory;
    private final CoordinateBasedPluginDownloader pluginDownloader;
    private final UrlResolverFactory urlResolverFactory;
    private final PluginInstaller pluginInstaller;
    private final String adminUsername;
    private final String adminPassword;

    public DefaultPluginManager(@Nonnull final OSGIKillbillAPI killbillApi, @Nonnull final KpmProperties kpmProperties) {
        this.killbillApi = killbillApi;
        this.urlResolverFactory = new UrlResolverFactory(kpmProperties);
        this.pluginIdentifiersDAO = createPluginIdentifiersDAO(kpmProperties);
        this.httpClient = createHttpClient(kpmProperties);
        this.availablePluginsComponentsFactory = new AvailablePluginsComponentsFactory(killbillApi, httpClient, kpmProperties);
        this.pluginDownloader = createCoordinateBasedPluginDownloader(kpmProperties);
        this.pluginInstaller = new DefaultPluginInstaller(new DefaultPluginFileService(kpmProperties));

        this.adminUsername = kpmProperties.getKillbillAdminUsername();
        this.adminPassword = kpmProperties.getKillbillAdminPassword();
    }

    @VisibleForTesting
    PluginIdentifiersDAO createPluginIdentifiersDAO(final KpmProperties kpmProperties) {
        return new FileBasedPluginIdentifiersDAO(kpmProperties);
    }

    @VisibleForTesting
    KPMClient createHttpClient(final KpmProperties kpmProperties) {
        try {
            // If exceptions are thrown here, the plugin cannot work properly in the first place
            return new KPMClient(kpmProperties.isStrictSSL(),
                                 kpmProperties.getConnectTimeoutSec() * 1000,
                                 kpmProperties.getReadTimeoutSec() * 1000);
        } catch (final GeneralSecurityException e) {
            throw new KPMPluginException("Cannot create KpmHttpClient, there's problem with SSL context creation.", e);
        }
    }

    CoordinateBasedPluginDownloader createCoordinateBasedPluginDownloader(final KpmProperties kpmProperties) {
        final ArtifactAndVersionFinder finder = new ArtifactAndVersionFinder(pluginIdentifiersDAO, availablePluginsComponentsFactory);
        final UriResolver uriResolver = urlResolverFactory.getCoordinateBasedPluginDownloaderUrlResolver();
        final boolean verifySSH = kpmProperties.pluginsInstall().coordinate().isVerifySHA1();
        final boolean alwaysTryPublicRepo = kpmProperties.pluginsInstall().coordinate().isAlwaysTryPublicRepository();
        return new CoordinateBasedPluginDownloader(httpClient, finder, uriResolver, verifySSH, alwaysTryPublicRepo);
    }

    private void notifyFileSystemChange(final PluginStateChange newState,
                                        final String pluginKey,
                                        final String pluginVersion) {
        try {
            logger.info("Notifying Kill Bill: state='{}', pluginKey='{}', pluginVersion={}", newState, pluginKey, pluginVersion);

            killbillApi.getSecurityApi().login(adminUsername, adminPassword);

            final PluginNamingResolver pluginNamingResolver = PluginNamingResolver.of(pluginKey);
            killbillApi.getPluginsInfoApi().notifyOfStateChanged(newState,
                                                                 pluginKey,
                                                                 pluginNamingResolver.getPluginName(),
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
            httpClient.download(uri, downloadedFile);

            // install
            pluginInstaller.install(downloadedFile, pluginKey, fixedVersion);

            // Add/update plugin identifier
            pluginIdentifiersDAO.add(pluginKey, fixedVersion);

            notifyFileSystemChange(PluginStateChange.NEW_VERSION, pluginKey, fixedVersion);

        } catch (final InvalidRequest e) {
            String responseInfo = "";
            if (e.getResponse() != null) {
                responseInfo += "HTTP status: " + e.getResponse().statusCode() + ". ";
            }

            responseInfo += "Error: " + e.getMessage();

            logger.error("Invalid request during plugin installation: URI={}, key={}, version={}. {}",
                         uri, pluginKey, pluginVersion, responseInfo, e);

            throw new KPMPluginException("Invalid request for URI: " + uri + ". " + responseInfo, e);
        } catch (final InterruptedException e) {
            logger.error("Plugin installation was interrupted: URI={}, key={}, version={}",
                         uri, pluginKey, pluginVersion, e);

            throw new KPMPluginException("Plugin installation was interrupted", e);
        } catch (final URISyntaxException e) {
            logger.error("Invalid URI syntax: URI={}, key={}, version={}",
                         uri, pluginKey, pluginVersion, e);

            throw new KPMPluginException("Invalid URI syntax: " + uri + ". Verify the URL is correctly formatted.", e);
        } catch (final IOException e) {
            logger.error("I/O error during plugin installation: URI={}, key={}, version={}",
                         uri, pluginKey, pluginVersion, e);

            throw new KPMPluginException("I/O error occurred during plugin installation. Check file system and network connectivity.", e);
        } catch (final Exception e) {
            logger.error("Unexpected error during plugin installation: URI={}, key={}, version={}.",
                         uri, pluginKey, pluginVersion, e);

            throw new KPMPluginException("Unexpected error occurred during plugin installation", e);
        } finally {
            final Path downloadDir = downloadedFile == null ? null : downloadedFile.getParent();
            FilesUtils.deleteIfExists(downloadedFile);
            FilesUtils.deleteIfExists(downloadDir);
        }
    }

    @Override
    public void install(@Nonnull final String pluginKey,
                        @Nonnull final String kbVersion,
                        String groupId,
                        String artifactId,
                        String pluginVersion,
                        final boolean forceDownload) throws KPMPluginException {
        logger.info("Install plugin via coordinate. key:{}, kbVersion:{}, version:{}, groupId:{}, artifactId:{}",
                    pluginKey, kbVersion, pluginVersion, groupId, artifactId);

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

            installedPath = pluginInstaller.install(downloadedPath, pluginKey, pluginVersion);

            // Add/update plugin identifier
            pluginIdentifiersDAO.add(pluginKey, groupId, artifactId, pluginVersion);

            notifyFileSystemChange(PluginStateChange.NEW_VERSION, pluginKey, pluginVersion);
            logger.info("Plugin key: {} installed successfully via coordinate.", pluginKey);
        } catch (final Exception e) {
            logger.error("Error when install pluginKey: '{}' with coordinate. Exception: {}", pluginKey, e.getMessage());
            // If exception happened, installed file should be deleted.
            FilesUtils.deleteIfExists(installedPath);
            throw new KPMPluginException(e);
        } finally {
            if (downloadResult != null && downloadResult.getDownloadedPath() != null) {
                FilesUtils.deleteRecursively(downloadResult.getDownloadedPath().getParent());
            }
        }
    }

    @Override
    public void uninstall(final String pluginKey, final String version) throws KPMPluginException {
        // Uninstall from bundlesPath
        final Path nextPluginByKey = pluginInstaller.uninstall(pluginKey, version);
        // Update plugin_identifiers.json
        final String nextPluginVersion = (nextPluginByKey == null || nextPluginByKey.getFileName() == null) ?
                                         null :
                                         PluginNamingResolver.getVersionFromString(nextPluginByKey.getFileName().toString());
        if (Strings.isNullOrEmpty(nextPluginVersion)) {
            // Last plugin by pluginKey. Just remove it.
            pluginIdentifiersDAO.remove(pluginKey);
        } else {
            // Replace the version value
            pluginIdentifiersDAO.add(pluginKey, nextPluginVersion);
        }

        // What if notifyFileSystemChange() implementation fails? Like, wrong username/password?
        notifyFileSystemChange(PluginStateChange.DISABLED, pluginKey, version);
    }
}
