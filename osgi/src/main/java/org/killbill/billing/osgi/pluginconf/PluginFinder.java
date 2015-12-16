/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
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
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.killbill.billing.osgi.api.config.PluginConfig;
import org.killbill.billing.osgi.api.config.PluginJavaConfig;
import org.killbill.billing.osgi.api.config.PluginLanguage;
import org.killbill.billing.osgi.api.config.PluginRubyConfig;
import org.killbill.billing.osgi.config.OSGIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PluginFinder {

    static final String SELECTED_VERSION_LINK_NAME = "ACTIVE";
    static final String TMP_DIR_NAME = "tmp";
    static final String DISABLED_FILE_NAME = "stop.txt"; // See similar definition in KillbillActivatorBase
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

    public List<PluginRubyConfig> getLatestRubyPlugins() throws PluginConfigException, IOException {
        return getLatestPluginForLanguage(PluginLanguage.RUBY);
    }

    public <T extends PluginConfig> List<T> getVersionsForPlugin(final String lookupName, @Nullable String version) throws PluginConfigException, IOException {
        loadPluginsIfRequired(false);

        final List<T> result = new LinkedList<T>();
        for (final String pluginName : allPlugins.keySet()) {
            if (pluginName.equals(lookupName)) {
                for (final PluginConfig cur : allPlugins.get(pluginName)) {
                    if (version == null || cur.getVersion().equals(version)) {
                        result.add((T) cur);
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
        return allPlugins;
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
        for (final String pluginName : allPlugins.keySet()) {
            final T plugin = (T) allPlugins.get(pluginName).get(0);
            if (pluginLanguage != plugin.getPluginLanguage()) {
                continue;
            }
            result.add(plugin);
        }

        return result;
    }

    private <T extends PluginConfig> void loadPluginsIfRequired(final boolean reloadPlugins) throws PluginConfigException, IOException {
        synchronized (allPlugins) {

            if (!reloadPlugins && allPlugins.size() > 0) {
                return;
            }

            allPlugins.clear();

            readPluginIdentifiers();

            loadPluginsForLanguage(PluginLanguage.RUBY);
            loadPluginsForLanguage(PluginLanguage.JAVA);

            // Order for each plugin  based on DefaultPluginConfig sort method:
            // (order first based on SELECTED_VERSION_LINK_NAME and then decreasing version number)
            //
            final Iterator<String> pluginNamesIterator = allPlugins.keySet().iterator();
            while (pluginNamesIterator.hasNext()) {
                final String pluginName = pluginNamesIterator.next();
                final LinkedList<PluginConfig> versionsForPlugin = allPlugins.get(pluginName);
                // If all entries were disabled or the SELECTED_VERSION_LINK_NAME was disabled we end up with nothing, it is as if the plugin did not exist
                if (versionsForPlugin.isEmpty()) {
                    pluginNamesIterator.remove();
                    continue;
                }

                Collections.sort(versionsForPlugin);
                // Make sure first entry is set with isSelectedForStart = true
                final PluginConfig firstValue = versionsForPlugin.removeFirst();
                final PluginConfig newFirstValue = firstValue.getPluginLanguage() == PluginLanguage.RUBY ?
                                                   new DefaultPluginRubyConfig((DefaultPluginRubyConfig) firstValue, true) :
                                                   new DefaultPluginJavaConfig((DefaultPluginJavaConfig) firstValue, true);
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
            final Map<String, PluginIdentifier> map = mapper.readValue(identifierFile, new TypeReference<Map<String, PluginIdentifier>>() {});
            identifiers.putAll(map);
        } catch (final IOException e) {
            logger.warn("Exception when parsing " + IDENTIFIERS_FILE_NAME + ":", e);
            return;
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
        for (String key : identifiers.keySet()) {
            PluginIdentifier value = identifiers.get(key);
            if (value.getPluginName().equals(pluginName) && value.getLanguage().equalsIgnoreCase(pluginLanguage.name())) {
                return key;
            }
        }
        return null;
    }

    private <T extends PluginConfig> T extractPluginConfig(final PluginLanguage pluginLanguage, final String pluginName, final String pluginVersion, final File pluginVersionDir, final boolean isVersionToStartLink) throws PluginConfigException {
        final T result;
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
            case RUBY:
                result = (T) new DefaultPluginRubyConfig(pluginKey, pluginName, pluginVersion, pluginVersionDir, props, isVersionToStartLink, isPluginDisabled(pluginVersionDir));
                break;
            case JAVA:
                result = (T) new DefaultPluginJavaConfig(pluginKey, pluginName, pluginVersion, pluginVersionDir, (props == null) ? new Properties() : props, isVersionToStartLink, isPluginDisabled(pluginVersionDir));
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
        final BufferedReader br = new BufferedReader(new FileReader(config));
        String line;
        while ((line = br.readLine()) != null) {
            final String[] parts = line.split("\\s*=\\s*");
            final String key = parts[0];
            final String value = parts[1];
            props.put(key, value);
        }
        br.close();
        return props;
    }
}
