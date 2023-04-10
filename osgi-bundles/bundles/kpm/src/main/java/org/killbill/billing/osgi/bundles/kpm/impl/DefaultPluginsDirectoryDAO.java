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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.PluginsDirectoryDAO;
import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.killbill.commons.utils.Strings;

/**
 * Will get available plugin information from "plugins_directory.yml" file. The YAML file structure that able to process
 * by this class is following
 * <a href="https://raw.githubusercontent.com/killbill/killbill-cloud/master/kpm/lib/kpm/plugins_directory.yml">this file</a>.
 */
class DefaultPluginsDirectoryDAO implements PluginsDirectoryDAO {

    private final KPMClient httpClient;
    private final String killbillVersion;
    private final String pluginDirectoryYmlUrl;

    DefaultPluginsDirectoryDAO(final KPMClient httpClient, final String semverBasedKbVersion, final String pluginDirectoryYmlUrl) {
        this.httpClient = httpClient;
        this.killbillVersion = semverBasedKbVersion;
        this.pluginDirectoryYmlUrl = pluginDirectoryYmlUrl;
    }

    @Override
    public Set<PluginsDirectoryModel> getPlugins() throws KPMPluginException {
        final Set<PluginsDirectoryModel> result = new HashSet<>();
        final Path downloadedPluginDirPath = downloadPluginDirectory();
        final YamlParser yamlParser = new YamlParser(downloadedPluginDirPath);
        final Set<Object> yamlData = yamlParser.loadAll();

        if (yamlData == null || yamlData.isEmpty()) {
            FilesUtils.deleteIfExists(downloadedPluginDirPath);
            return Collections.emptySet();
        }

        mapYamlDataToModel(yamlData, result);

        FilesUtils.deleteIfExists(downloadedPluginDirPath);

        return result;
    }

    Path downloadPluginDirectory() {
        try {
            return httpClient.downloadArtifactMetadata(pluginDirectoryYmlUrl);
        } catch (final Exception e) {
            throw new KPMPluginException(String.format("Cannot get plugin directory YAML from URL: %s", pluginDirectoryYmlUrl), e);
        }
    }

    void mapYamlDataToModel(final Iterable<Object> yamlData, final Collection<PluginsDirectoryModel> result) {
        yamlData.forEach(allData -> {
            final Map<String, Object> root = (Map<String, Object>) allData;
            for (final Entry<String, Object> node : root.entrySet()) {
                // accertify, adyen, analytics, etc
                final String pluginKey = node.getKey().replace(":", "");
                final Object value = node.getValue();
                if (value instanceof Map) {
                    final Map<String, Object> typeVersionsTree = (Map<String, Object>) value;
                    final Map<String, String> versions = (Map<String, String>) typeVersionsTree.get(":versions");
                    final Object artifactId = typeVersionsTree.get(":artifact_id");
                    for (final Entry<String, String> versionEntry : versions.entrySet()) {
                        final String compatibleKbVersion = versionEntry.getKey().replace(":", "");
                        // "plugins_directory.yml" only contains MAJOR.MINOR killbill version. So use ".startsWith()" here
                        if (killbillVersion.startsWith(compatibleKbVersion)) {
                            final PluginsDirectoryModel model = new PluginsDirectoryModel(pluginKey, versionEntry.getValue());
                            if (artifactId != null && !Strings.isNullOrEmpty(artifactId.toString())) {
                                model.setPluginArtifactId(artifactId.toString());
                            }
                            result.add(model);
                        }
                    }
                }
            }
        });
    }
}
