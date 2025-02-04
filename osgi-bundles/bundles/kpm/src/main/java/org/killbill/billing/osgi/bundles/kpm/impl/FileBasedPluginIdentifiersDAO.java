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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.KpmProperties;
import org.killbill.billing.osgi.bundles.kpm.PluginIdentifiersDAO;
import org.killbill.commons.utils.Preconditions;
import org.killbill.commons.utils.Strings;
import org.killbill.commons.utils.annotation.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

class FileBasedPluginIdentifiersDAO implements PluginIdentifiersDAO {

    private static final Logger logger = LoggerFactory.getLogger(FileBasedPluginIdentifiersDAO.class);
    private static final String FILE_NAME = "plugin_identifiers.json";

    private final ObjectMapper objectMapper;

    @VisibleForTesting
    final File file;

    public FileBasedPluginIdentifiersDAO(final KpmProperties kpmProperties) {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        final Path bundlesPath = kpmProperties.getBundlesPath();
        final Path directory = Path.of(bundlesPath.toString(), "plugins");

        try {
            Files.createDirectories(directory);
            file = Path.of(directory.toString(), FILE_NAME).toFile();
            if (file.createNewFile()) {
                objectMapper.writeValue(file, Collections.emptyMap());
            }
        } catch (final IOException e) {
            throw new KPMPluginException(e);
        }
    }

    @VisibleForTesting
    Map<String, PluginIdentifierModel> loadFileContent() {
        try {
            final Map<String, PluginIdentifierModel> emptyPluginMap = new HashMap<>();

            if (!file.exists() || file.length() == 0 || Files.readString(file.toPath()).isBlank()) {
                logger.info("File {} is missing or empty. Initializing with an empty JSON object", file.getAbsolutePath());

                objectMapper.writeValue(file, emptyPluginMap);

                return emptyPluginMap;
            }

            return objectMapper.readValue(file, new TypeReference<>() {});
        } catch (final IOException e) {
            throw new KPMPluginException(String.format("Cannot load %s content", file), e);
        }
    }

    @VisibleForTesting
    void writeContentToFile(final Map<String, PluginIdentifierModel> contents) {
        try {
            objectMapper.writeValue(file, contents);
        } catch (final IOException e) {
            throw new KPMPluginException(String.format("Cannot write to %s. Content value: %s", file, contents.toString()), e);
        }
    }

    @Override
    public void add(final String pluginKey, final String version) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(pluginKey), "'pluginKey' cannot be null or empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(version), "'version' cannot be null or empty");

        final PluginNamingResolver namingResolver = PluginNamingResolver.of(pluginKey, version);

        final Map<String, PluginIdentifierModel> content = loadFileContent();
        content.put(pluginKey, new PluginIdentifierModel(namingResolver.getPluginName(), namingResolver.getPluginVersion()));

        writeContentToFile(content);
    }

    @Override
    public void add(final String pluginKey, final String groupId, final String artifactId, final String version) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(pluginKey), "'pluginKey' cannot be null or empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(groupId), "'groupId' cannot be null or empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(artifactId), "'artifactId' cannot be null or empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(version), "'version' cannot be null or empty");

        final PluginNamingResolver namingResolver = PluginNamingResolver.of(pluginKey, version);

        final Map<String, PluginIdentifierModel> content = loadFileContent();
        final PluginIdentifierModel model = new PluginIdentifierModel(namingResolver.getPluginName(),
                                                                      groupId,
                                                                      artifactId,
                                                                      namingResolver.getPluginVersion());
        content.put(pluginKey, model);

        writeContentToFile(content);
    }

    @Override
    public void remove(final String pluginKey) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(pluginKey), "'pluginKey' cannot be null or empty");

        final Map<String, PluginIdentifierModel> content = loadFileContent();
        content.remove(pluginKey);

        writeContentToFile(content);
    }

    @Override
    public Set<PluginIdentifiersModel> getPluginIdentifiers() {
        return loadFileContent().entrySet()
                                .stream()
                                .map(entry -> new PluginIdentifiersModel(entry.getKey(), entry.getValue()))
                                .collect(Collectors.toUnmodifiableSet());
    }
}
