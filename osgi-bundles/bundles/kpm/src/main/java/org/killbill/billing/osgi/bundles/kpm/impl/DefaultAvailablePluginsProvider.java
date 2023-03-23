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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.killbill.billing.osgi.bundles.kpm.AvailablePluginsProvider;
import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.yaml.snakeyaml.Yaml;

/**
 * Will get available plugin information from "plugins_directory.yml" file. The YAML file structure that able to process
 * by this class is following
 * <a href="https://raw.githubusercontent.com/killbill/killbill-cloud/master/kpm/lib/kpm/plugins_directory.yml">this file</a>.
 */
class DefaultAvailablePluginsProvider implements AvailablePluginsProvider {

    private final String killbillVersion;
    private final List<Object> yamlData;

    public DefaultAvailablePluginsProvider(final KPMClient kpmClient,
                                           final String semverBasedKbVersion,
                                           final String pluginDirectoryYamlUrl) throws Exception {
        this.killbillVersion = semverBasedKbVersion;
        final Path pluginDirectoryYml = kpmClient.downloadArtifactMetadata(pluginDirectoryYamlUrl);
        final Yaml yaml = new Yaml();
        yamlData = new ArrayList<>();
        // yaml.loadAll() return Iterable<Object> which is a "view" and will empty once consumed
        yaml.loadAll(Files.readString(pluginDirectoryYml)).forEach(yamlData::add);

        Files.deleteIfExists(pluginDirectoryYml);
    }

    @Override
    public Set<AvailablePluginsModel> getAvailablePlugins() {
        final Set<AvailablePluginsModel> result = new HashSet<>();
        if (yamlData == null || yamlData.isEmpty()) {
            return Collections.emptySet();
        }

        yamlData.forEach(allData -> {
            final Map<String, Object> root = (Map<String, Object>) allData;
            for (final Entry<String, Object> node : root.entrySet()) {
                // accertify, adyen, analytics, etc
                final String pluginKey = node.getKey().replace(":", "");
                final Object value = node.getValue();
                if (value instanceof Map) {
                    final Map<String, Object> typeVersionsTree = (Map<String, Object>) value;
                    final Map<String, String> versions = (Map<String, String>) typeVersionsTree.get(":versions");
                    for (final Entry<String, String> versionEntry : versions.entrySet()) {
                        final String compatibleKbVersion = versionEntry.getKey().replace(":", "");
                        // "plugins_directory.yml" only contains MAJOR.MINOR killbill version. So use ".startsWith()" here
                        if (killbillVersion.startsWith(compatibleKbVersion)) {
                            result.add(new AvailablePluginsModel(pluginKey, versionEntry.getValue()));
                        }
                    }
                }
            }
        });
        return result;
    }
}
