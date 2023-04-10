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

import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import org.killbill.billing.osgi.api.OSGIKillbill;
import org.killbill.billing.osgi.bundles.kpm.PluginsDirectoryDAO;
import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.NexusMetadataFiles;
import org.killbill.billing.osgi.bundles.kpm.PluginManager;
import org.killbill.billing.osgi.bundles.kpm.PluginsDirectoryDAO.PluginsDirectoryModel;
import org.killbill.billing.osgi.bundles.kpm.VersionsProvider;
import org.killbill.billing.util.nodes.NodeInfo;
import org.killbill.commons.utils.Preconditions;
import org.killbill.commons.utils.Strings;
import org.killbill.commons.utils.cache.Cache;
import org.killbill.commons.utils.cache.DefaultCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instantiate components needed to fulfill {@link PluginManager#getAvailablePlugins(String, boolean)}.
 *
 * @see VersionsProvider
 * @see PluginsDirectoryDAO
 */
public final class AvailablePluginsComponentsFactory {

    private static final int DEFAULT_CACHE_SIZE = 10;

    private static final int DEFAULT_CACHE_EXPIRATION_SEC = 86400; // 24 hours

    private final Logger logger = LoggerFactory.getLogger(AvailablePluginsComponentsFactory.class);

    private final OSGIKillbill osgiKillbill;
    private final KPMClient httpClient;
    private final String nexusUrl;
    private final String nexusRepository;
    private final String pluginDirectoryUrl;
    private final boolean bypassCache;

    private final Cache<CacheKey, VersionsProvider> versionsProviderCache;
    private final Cache<CacheKey, Set<PluginsDirectoryModel>> pluginDirectoryCache;

    public AvailablePluginsComponentsFactory(final OSGIKillbill osgiKillbill, final KPMClient httpClient, final Properties properties) {
        this.osgiKillbill = osgiKillbill;
        this.httpClient = httpClient;

        nexusUrl = Objects.requireNonNullElse(properties.getProperty(PluginManager.PROPERTY_PREFIX + "nexusUrl"), "https://oss.sonatype.org");
        nexusRepository = Objects.requireNonNullElse(properties.getProperty(PluginManager.PROPERTY_PREFIX + "nexusRepository"), "releases");
        pluginDirectoryUrl = Objects.requireNonNullElse(
                properties.getProperty(PluginManager.PROPERTY_PREFIX + "availablePlugins.pluginDirectoryUrl"),
                PluginsDirectoryDAO.DEFAULT_DIRECTORY);

        final int cacheSize = Objects.requireNonNullElse(
                Integer.getInteger(properties.getProperty(PluginManager.PROPERTY_PREFIX + "availablePlugins.cache.size")),
                DEFAULT_CACHE_SIZE);
        final int expirationSec = Objects.requireNonNullElse(
                Integer.getInteger(properties.getProperty(PluginManager.PROPERTY_PREFIX + "availablePlugins.cache.expirationSecs")),
                DEFAULT_CACHE_EXPIRATION_SEC);
        bypassCache = Boolean.parseBoolean(properties.getProperty(PluginManager.PROPERTY_PREFIX + "availablePlugins.cache.bypass"));

        if (!bypassCache) {
            // We cant set cache loaders in constructor. It's pretty expensive.
            versionsProviderCache = new DefaultCache<>(cacheSize, expirationSec, DefaultCache.noCacheLoader());
            pluginDirectoryCache = new DefaultCache<>(cacheSize, expirationSec, DefaultCache.noCacheLoader());
        } else {
            versionsProviderCache = null;
            pluginDirectoryCache = null;
        }
    }

    Function<CacheKey, VersionsProvider> versionsProviderLoader() {
        return key -> {
            final NexusMetadataFiles nexusMetadataFiles = createNexusMetadataFiles(key.getVersion());
            final NodeInfo nodeInfo = osgiKillbill.getKillbillNodesApi().getCurrentNodeInfo();
            try {
                return nodeInfo.getKillbillVersion().equals(key.getVersion()) ?
                       new DefaultVersionsProvider(nexusMetadataFiles, nodeInfo) :
                       new DefaultVersionsProvider(nexusMetadataFiles);
            } catch (final Exception e) {
                throw new KPMPluginException(String.format("Unable to get killbill version info: %s", key.getVersion()), e);
            }
        };
    }

    Function<CacheKey, Set<PluginsDirectoryModel>> pluginDirectoryLoader() {
        return key -> {
            final PluginsDirectoryDAO pluginsDirectoryDAO = new DefaultPluginsDirectoryDAO(httpClient, key.getVersion(), key.getUrl());
            return pluginsDirectoryDAO.getPlugins();
        };
    }

    /**
     * Create {@link VersionsProvider} instance. This is an expensive operation because involved one or more HTTP call.
     *
     * @param killbillVersionOrLatest valid, sem-ver compliance killbill version, or 'LATEST'.
     * @param forceDownload when it false, we just try to load from cache and no download operation
     */
    public VersionsProvider createVersionsProvider(final String killbillVersionOrLatest, final boolean forceDownload) throws KPMPluginException {
        logger.debug("#createVersionsProvider() with version:{}, forceDownload:{}", killbillVersionOrLatest, forceDownload);

        final CacheKey cacheKey = CacheKey.of(killbillVersionOrLatest, nexusUrl);

        if (bypassCache) {
            return versionsProviderLoader().apply(cacheKey);
        }
        return versionsProviderCache.getOrLoad(cacheKey, forceDownload ? versionsProviderLoader() : key -> VersionsProvider.ZERO);
    }

    private NexusMetadataFiles createNexusMetadataFiles(final String killbillVersionOrLatest) {
        // FIXME-TS-58: This FIXME updated. See more here:
        //   https://github.com/killbill/killbill-platform/pull/134#discussion_r1144512376
        //   (or see commit history for initial content)
        return new SonatypeNexusMetadataFiles(httpClient, nexusUrl, nexusRepository, killbillVersionOrLatest);
    }

    /**
     * Create instance of {@link PluginsDirectoryDAO}. Parameter passed in {@code fixedKillbillVersion} should be
     * valid sem-ver semantics. Attempt to set {@code LATEST} to parameter will immediately throw an
     * {@code IllegalArgumentException}. If version really unknown, client code could call
     * {@link #createVersionsProvider(String, boolean)} and then use {@link VersionsProvider#getFixedKillbillVersion()}.
     *
     * @param fixedKillbillVersion sem-ver killbill version
     * @param forceDownload when it false, we just try to load from cache and no download operation
     */
    // FIXME-TS-58: There's request to add ability to set plugin_directory.yml file at runtime, ex via request parameter
    //   like: http://127.0.0.1:8080/plugins/killbill-kpm/plugins?latest=true&pluginDirUrl=https://plugindir.com/file.yml
    //   although this is doable, some concern are:
    //   1. How this affected technical-support-93 (https://github.com/killbill/technical-support/issues/93) ?
    //   2. This probably make an open to overflow attack, where unexpected YAML URL passed and exception thrown multiple times
    public PluginsDirectoryDAO createPluginsDirectoryDAO(final String fixedKillbillVersion, final boolean forceDownload) throws KPMPluginException {
        logger.debug("#createPluginsDirectoryDAO() with killbillVersion: {} and forceDownload: {}", fixedKillbillVersion, forceDownload);
        // For validating version format
        final String version = PluginNamingResolver.getVersionFromString(fixedKillbillVersion);
        if (Strings.isNullOrEmpty(version)) {
            throw new IllegalArgumentException(String.format("'%s' is not a valid killbill version in createAvailablePluginsProvider()", fixedKillbillVersion));
        }

        final CacheKey cacheKey = CacheKey.of(version, pluginDirectoryUrl);

        if (bypassCache) {
            return () -> pluginDirectoryLoader().apply(cacheKey);
        }

        return () -> pluginDirectoryCache.getOrLoad(cacheKey, forceDownload ? pluginDirectoryLoader() : key -> PluginsDirectoryDAO.NONE.getPlugins());
    }

    /**
     * Originally, "version" variable, which is usually just simply java.lang.String, used as cache key. But turns out
     * that the same version could contain different metadata info based on where the metadata info located (the URL).
     */
    static class CacheKey {
        private final String version;
        private final String url;

        CacheKey(final String version, final String url) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(version), "version in CacheKey is null or empty");
            Preconditions.checkArgument(!Strings.isNullOrEmpty(url), "url in CacheKey is null or empty");
            this.version = version;
            this.url = url;
        }

        static CacheKey of(final String version, final String url) {
            return new CacheKey(version, url);
        }

        public String getVersion() {
            return version;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final CacheKey cacheKey = (CacheKey) o;
            return version.equals(cacheKey.version) && url.equals(cacheKey.url);
        }

        @Override
        public int hashCode() {
            return Objects.hash(version, url);
        }
    }
}
