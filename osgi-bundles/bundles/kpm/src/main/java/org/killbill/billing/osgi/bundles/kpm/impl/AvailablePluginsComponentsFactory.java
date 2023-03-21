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

import org.killbill.billing.osgi.api.OSGIKillbill;
import org.killbill.billing.osgi.bundles.kpm.AvailablePluginsProvider;
import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.NexusMetadataFiles;
import org.killbill.billing.osgi.bundles.kpm.PluginManager;
import org.killbill.billing.osgi.bundles.kpm.VersionsProvider;
import org.killbill.billing.util.nodes.NodeInfo;
import org.killbill.commons.utils.Strings;
import org.killbill.commons.utils.cache.Cache;
import org.killbill.commons.utils.cache.DefaultCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instantiate components needed to fulfill {@link PluginManager#getAvailablePlugins(String, boolean)}.
 *
 * @see VersionsProvider
 * @see AvailablePluginsProvider
 */
public final class AvailablePluginsComponentsFactory {

    private final Logger logger = LoggerFactory.getLogger(AvailablePluginsComponentsFactory.class);

    private static final int CACHE_SIZE = 10;

    private final OSGIKillbill osgiKillbill;
    private final KPMClient httpClient;
    private final String nexusUrl;
    private final String nexusRepository;

    private final Cache<String, VersionsProvider> versionsProviderCache;
    private final Cache<String, AvailablePluginsProvider> pluginsProviderCache;

    public AvailablePluginsComponentsFactory(final OSGIKillbill osgiKillbill,
                                             final KPMClient httpClient,
                                             final String nexusUrl,
                                             final String nexusRepository) {
        this.osgiKillbill = osgiKillbill;
        this.httpClient = httpClient;
        this.nexusUrl = nexusUrl;
        this.nexusRepository = nexusRepository;

        versionsProviderCache = new DefaultCache<>(CACHE_SIZE);
        pluginsProviderCache = new DefaultCache<>(CACHE_SIZE);
    }

    /**
     * Create {@link VersionsProvider} instance. This is an expensive operation because involved one or more HTTP call.
     *
     * @param killbillVersionOrLatest valid, sem-ver compliance killbill version, or 'LATEST'.
     * @param forceDownload when it false, we just try to load from cache and no download operation
     */
    public VersionsProvider createVersionsProvider(final String killbillVersionOrLatest, final boolean forceDownload) throws KPMPluginException {
        logger.debug("#createVersionsProvider() with version:{}, forceDownload:{}", killbillVersionOrLatest, forceDownload);

        final VersionsProvider result = versionsProviderCache.get(killbillVersionOrLatest);
        if (result == null && forceDownload) {
            final NexusMetadataFiles nexusMetadataFiles = createNexusMetadataFiles(killbillVersionOrLatest);
            final NodeInfo nodeInfo = osgiKillbill.getKillbillNodesApi().getCurrentNodeInfo();
            try {
                // NodeInfo doesn't have killbill-oss-parent version info, but have other killbill libs info. Thus,
                // adding NodeInfo option here worth it because we can reduce remote call to just 1 call, since
                // killbill-oss-parent.pom is not needed anymore (get covered by NodeInfo)
                if (nodeInfo.getKillbillVersion().equals(killbillVersionOrLatest)) {
                    versionsProviderCache.put(killbillVersionOrLatest, new DefaultVersionsProvider(nexusMetadataFiles, nodeInfo));
                } else {
                    versionsProviderCache.put(killbillVersionOrLatest, new DefaultVersionsProvider(nexusMetadataFiles));
                }
                return versionsProviderCache.get(killbillVersionOrLatest);
            } catch (final Exception e) {
                throw new KPMPluginException(String.format("Unable to get killbill version info: %s", killbillVersionOrLatest), e);
            }
        } else {
            return Objects.requireNonNullElse(result, VersionsProvider.ZERO);
        }
    }

    private NexusMetadataFiles createNexusMetadataFiles(final String killbillVersionOrLatest) {
        // FIXME-TS-58: In KPM, we have: KPM::Tasks::info(), and then in the end, KPM::NexusFacade::Actions.initialize()
        //   that used to determine which nexus server we need to get killbill metadata info. But in reality, calling
        //   "kpm info --as-json --force-download --version=0.24.0 --overrides=url:<ANY_URL> --overrides=repository:releases"
        //   and replace <ANY_URL> with "https://username:password@maven.pkg.github.com", "https://dl.cloudsmith.io" or
        //   "https://oss.sonatype.org" produce the same thing.
        //   -
        //   Maybe KPM::NexusFacade::Actions.initialize() only used in install plugin via artifact and not applied here?
        //   Should we throw an exception if nexusUrl is not "https://oss.sonatype.org" ?
        return new SonatypeNexusMetadataFiles(httpClient, nexusUrl, nexusRepository, killbillVersionOrLatest);
    }

    /**
     * Create instance of {@link AvailablePluginsProvider}. Parameter passed in {@code fixedKillbillVersion} should be
     * valid sem-ver semantics. Attempt to set {@code LATEST} to parameter will immediately throw an
     * {@code IllegalArgumentException}. If version really unknown, client code could call
     * {@link #createVersionsProvider(String, boolean)} and then use {@link VersionsProvider#getFixedKillbillVersion()}.
     *
     * @param fixedKillbillVersion sem-ver killbill version
     * @param forceDownload when it false, we just try to load from cache and no download operation
     */
    public AvailablePluginsProvider createAvailablePluginsProvider(final String fixedKillbillVersion, final boolean forceDownload) throws KPMPluginException {
        logger.debug("#createAvailablePluginsProvider() with killbillVersion: {} and forceDownload: {}", fixedKillbillVersion, forceDownload);
        // For validating version format
        final String version = PluginNamingResolver.getVersionFromString(fixedKillbillVersion);
        if (Strings.isNullOrEmpty(version)) {
            throw new IllegalArgumentException(String.format("'%s' is not a valid killbill version in createAvailablePluginsProvider() method", fixedKillbillVersion));
        }

        final AvailablePluginsProvider result = pluginsProviderCache.get(version);
        if (result == null && forceDownload) {
            try {
                pluginsProviderCache.put(version, new DefaultAvailablePluginsProvider(httpClient, version));
            } catch (final Exception e) {
                throw new KPMPluginException(String.format("Unable to get available plugin info for killbill version: %s", version), e);
            }
            return pluginsProviderCache.get(version);
        } else {
            return Objects.requireNonNullElse(result, AvailablePluginsProvider.NONE);
        }
    }
}
