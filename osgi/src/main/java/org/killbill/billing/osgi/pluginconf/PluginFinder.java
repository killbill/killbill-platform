/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

package org.killbill.billing.osgi.pluginconf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.killbill.billing.osgi.api.config.PluginConfig;
import org.killbill.billing.osgi.api.config.PluginJavaConfig;
import org.killbill.billing.osgi.api.config.PluginLanguage;
import org.killbill.billing.osgi.config.OSGIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PluginFinder {

    static final String SELECTED_VERSION_LINK_NAME = "SET_DEFAULT";
    static final String TMP_DIR_NAME = "tmp";
    static final String DISABLED_FILE_NAME = "disabled.txt"; // See similar definition in KillbillActivatorBase
    static final String IDENTIFIERS_FILE_NAME = "plugin_identifiers.json";

    private final Logger logger = LoggerFactory.getLogger(PluginFinder.class);

    private final OSGIConfig osgiConfig;
    private final Map<String, LinkedList<PluginConfig>> allPlugins;
    private final Map<String, PluginIdentifier> identifiers;
    private final ObjectMapper mapper;

    @Inject
    public PluginFinder(final OSGIConfig osgiConfig) {
        this.osgiConfig = osgiConfig;
        this.mapper = new ObjectMapper();
        this.allPlugins = new HashMap<String, LinkedList<PluginConfig>>();
        this.identifiers = new HashMap<String, PluginIdentifier>();
    }

    public List<PluginJavaConfig> getLatestJavaPlugins() throws PluginConfigException, IOException {
        return getLatestPluginForLanguage(PluginLanguage.JAVA);
    }

    public List<PluginConfig> getVersionsForPlugin(final String lookupName, @Nullable final String version) throws PluginConfigException, IOException {
        loadPluginsIfRequired(false);

        final List<PluginConfig> result = new LinkedList<PluginConfig>();
        for (final Entry<String, LinkedList<PluginConfig>> entry : allPlugins.entrySet()) {
            if (entry.getKey().equals(lookupName)) {
                for (final PluginConfig cur : entry.getValue()) {
                    if (version == null || cur.getVersion().equals(version)) {
                        result.add(cur);
                    }
                }
            }
        }
        return result;
    }

    public String getPluginVersionSelectedForStart(final String pluginName) {
        final LinkedList<PluginConfig> pluginConfigs = allPlugins.get(pluginName);
        return pluginConfigs != null && !pluginConfigs.isEmpty() ? pluginConfigs.get(0).getVersion() : null;
    }

    public Map<String, LinkedList<PluginConfig>> getAllPlugins() {
        return Map.copyOf(allPlugins);
    }

    public void reloadPlugins() throws PluginConfigException, IOException {
        loadPluginsIfRequired(true);
    }

    public PluginIdentifier resolvePluginKey(final String pluginKey) {
        return identifiers.get(pluginKey);
    }

    private <T extends PluginConfig> List<T> getLatestPluginForLanguage(final PluginLanguage pluginLanguage) throws PluginConfigException, IOException {
        loadPluginsIfRequired(false);

        final List<T> result = new LinkedList<T>();
        for (final LinkedList<PluginConfig> plugins : allPlugins.values()) {
            @SuppressWarnings("unchecked") final T plugin = (T) plugins.get(0);
            if (pluginLanguage != plugin.getPluginLanguage()) {
                continue;
            }
            result.add(plugin);
        }

        return result;
    }

    private <T extends PluginConfig> void loadPluginsIfRequired(final boolean reloadPlugins) throws PluginConfigException, IOException {
        synchronized (allPlugins) {

            if (!reloadPlugins && !allPlugins.isEmpty()) {
                return;
            }

            allPlugins.clear();

            readPluginIdentifiers();

            loadPluginsForLanguage(PluginLanguage.JAVA);

            // Order for each plugin  based on DefaultPluginConfig sort method:
            // (order first based on SELECTED_VERSION_LINK_NAME and then decreasing version number)
            //
            for (final Entry<String, LinkedList<PluginConfig>> entry : allPlugins.entrySet()) {
                final String pluginName = entry.getKey();
                final LinkedList<PluginConfig> versionsForPlugin = entry.getValue();
                // If all entries were disabled or the SELECTED_VERSION_LINK_NAME was disabled we end up with nothing, it is as if the plugin did not exist
                if (versionsForPlugin.isEmpty()) {
                    allPlugins.remove(pluginName);
                    continue;
                }
                Collections.sort(versionsForPlugin);
                // Make sure first entry is set with isSelectedForStart = true
                final PluginConfig firstValue = versionsForPlugin.removeFirst();
                if (firstValue.getPluginLanguage() != PluginLanguage.JAVA) {
                    throw new UnsupportedOperationException("Non-Java plugins aren't supported anymore");
                }
                final PluginConfig newFirstValue = new DefaultPluginJavaConfig((DefaultPluginJavaConfig) firstValue, true);
                versionsForPlugin.addFirst(newFirstValue);
            }
        }
    }

    private void readPluginIdentifiers() {

        final String identifierFileName = osgiConfig.getRootInstallationDir() + "/plugins/" + IDENTIFIERS_FILE_NAME;
        final File identifierFile = new File(identifierFileName);
        if (!identifierFile.exists() || !identifierFile.isFile()) {
            logger.warn("File non existent: Skipping parsing of " + IDENTIFIERS_FILE_NAME);
            return;
        }

        try {
            identifiers.clear();

            if (identifierFile.length() == 0 || Files.readString(identifierFile.toPath()).isBlank()) {
                logger.info("File {} is missing or empty. Initializing with an empty JSON object", identifierFile.getAbsolutePath());

                mapper.writeValue(identifierFile, new HashMap<>());
            }
            final Map<String, PluginIdentifier> map = mapper.readValue(identifierFile, new TypeReference<>() {});
            identifiers.putAll(map);
        } catch (final IOException e) {
            logger.warn("Exception when parsing " + IDENTIFIERS_FILE_NAME + ":", e);
        }
    }

    private String resolveVersionToStartLink(final File pluginVersionsRoot) throws IOException {
        final File selectedVersionLink = new File(pluginVersionsRoot + "/" + SELECTED_VERSION_LINK_NAME);
        if (selectedVersionLink.exists() && selectedVersionLink.isDirectory()) {
            return selectedVersionLink.getCanonicalFile().getName();
        }
        return null;
    }

    private void loadPluginsForLanguage(final PluginLanguage pluginLanguage) throws PluginConfigException, IOException {
        final String rootDirPath = osgiConfig.getRootInstallationDir() + "/plugins/" + pluginLanguage.toString().toLowerCase();
        final File rootDir = new File(rootDirPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            logger.warn("Configuration root dir {} is not a valid directory", rootDirPath);
            return;
        }

        final File[] files = rootDir.listFiles();
        if (files == null) {
            return;
        }
        for (final File curPlugin : files) {
            // Skip any non directory entry
            if (!curPlugin.isDirectory()) {
                logger.warn("Skipping entry {} in directory {}", curPlugin.getName(), rootDir.getAbsolutePath());
                continue;
            }
            final String pluginName = curPlugin.getName();

            final File[] filesInDir = curPlugin.listFiles();
            if (filesInDir == null) {
                continue;
            }

            final String versionToStart = resolveVersionToStartLink(curPlugin);

            LinkedList<PluginConfig> curPluginVersionlist = allPlugins.get(pluginName);
            if (curPluginVersionlist == null) {
                curPluginVersionlist = new LinkedList<PluginConfig>();
                allPlugins.put(pluginName, curPluginVersionlist);
            }
            for (final File curVersion : filesInDir) {
                // Skip any non directory entry
                if (!curVersion.isDirectory()) {
                    logger.warn("Skipping entry {} in directory {}", curPlugin.getName(), rootDir.getAbsolutePath());
                    continue;
                }
                final String version = curVersion.getName();
                // Skip the symlink 'SELECTED_VERSION_LINK_NAME' if exists
                if (SELECTED_VERSION_LINK_NAME.equals(version)) {
                    continue;
                }
                final boolean isVersionToStartLink = versionToStart != null && versionToStart.equals(version);

                final PluginConfig plugin;
                try {
                    plugin = extractPluginConfig(pluginLanguage, pluginName, version, curVersion, isVersionToStartLink);
                } catch (final PluginConfigException e) {
                    logger.warn("Skipping plugin {}: {}", pluginName, e.getMessage());
                    continue;
                }
                // Add the entry if this is not marked as 'disabled'
                if (!plugin.isDisabled()) {
                    curPluginVersionlist.add(plugin);
                    logger.info("Adding plugin {} ", plugin.getPluginVersionnedName());
                }
            }
        }
    }

    private String findPluginKey(final String pluginName, final PluginLanguage pluginLanguage) {
        for (final Entry<String, PluginIdentifier> entry : identifiers.entrySet()) {
            if (entry.getValue().getPluginName().equals(pluginName) && entry.getValue().getLanguage().equalsIgnoreCase(pluginLanguage.name())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private PluginConfig extractPluginConfig(final PluginLanguage pluginLanguage, final String pluginName, final String pluginVersion, final File pluginVersionDir, final boolean isVersionToStartLink) throws PluginConfigException {
        final PluginConfig result;
        Properties props = null;
        try {
            final File[] files = pluginVersionDir.listFiles();
            if (files == null) {
                throw new PluginConfigException("Unable to list files in " + pluginVersionDir.getAbsolutePath());
            }

            for (final File cur : files) {
                if (cur.isFile() && cur.getName().equals(osgiConfig.getOSGIKillbillPropertyName())) {
                    props = readPluginConfigurationFile(cur);
                }
                if (props != null) {
                    break;
                }
            }

            if (pluginLanguage == PluginLanguage.RUBY && props == null) {
                throw new PluginConfigException("Invalid plugin configuration file for " + pluginName + "-" + pluginVersion);
            }

        } catch (final IOException e) {
            throw new PluginConfigException("Failed to read property file for " + pluginName + "-" + pluginVersion, e);
        }

        final String pluginKey = findPluginKey(pluginName, pluginLanguage);
        switch (pluginLanguage) {
            case JAVA:
                result = new DefaultPluginJavaConfig(pluginKey, pluginName, pluginVersion, pluginVersionDir, (props == null) ? new Properties() : props, isVersionToStartLink, isPluginDisabled(pluginVersionDir));
                break;
            default:
                throw new RuntimeException("Unknown plugin language " + pluginLanguage);
        }
        return result;
    }

    private boolean isPluginDisabled(final File pluginVersionDir) {
        final File disabledFile = new File(pluginVersionDir + "/" + TMP_DIR_NAME + "/" + DISABLED_FILE_NAME);
        return disabledFile.isFile();
    }

    private Properties readPluginConfigurationFile(final File config) throws IOException {
        final Properties props = new Properties();
        try (final InputStream in = new FileInputStream(config);
             final Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             final BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                final String[] parts = line.split("\\s*=\\s*");
                final String key = parts[0];
                final String value = parts[1];
                props.put(key, value);
            }
            return props;
        }
    }
}
