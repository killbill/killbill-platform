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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.PluginFileService;
import org.killbill.billing.osgi.bundles.kpm.PluginInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultPluginInstaller implements PluginInstaller {

    private final Logger logger = LoggerFactory.getLogger(DefaultPluginInstaller.class);

    private final PluginFileService pluginFileService;

    public DefaultPluginInstaller(final PluginFileService pluginFileService) {
        this.pluginFileService = pluginFileService;
    }

    /**
     * See point 6 of "Current Implementation" in <a href="https://github.com/killbill/technical-support/issues/92">this issue</a>.
     */
    @Override
    public Path install(final Path downloadedFile, final String pluginKey, final String pluginVersion) throws KPMPluginException {
        try {
            // Make directory
            final Path pluginDirectory = pluginFileService.createPluginDirectory(pluginKey, pluginVersion);

            // Copy downloadedFile to directory
            final Path result = Files.copy(downloadedFile,
                                           pluginDirectory.resolve(PluginNamingResolver.of(pluginKey, pluginVersion).getPluginJarFileName()),
                                           StandardCopyOption.REPLACE_EXISTING);

            // Make symlink
            pluginFileService.createSymlink(pluginDirectory);

            return result;
        } catch (final IOException e) {
            throw new KPMPluginException(String.format("Unable to install plugin with key: %s", pluginKey), e);
        }
    }

    @Override
    public Path uninstall(final String pluginKey, final String pluginVersion) throws KPMPluginException {
        try {
            final PluginNamingResolver namingResolver = PluginNamingResolver.of(pluginKey, pluginVersion);
            // Example value: <bundlesPath>/plugins/java/some-plugins/1.2.3/
            final Path pluginDirectory = pluginFileService.getPluginDirByPluginKeyAndVersion(pluginKey, pluginVersion);
            // Example value: <bundlesPath>/plugins/java/some-plugins/1.2.3/some-plugins-1.2.3.jar
            final Path pluginFile = pluginDirectory.resolve(namingResolver.getPluginJarFileName());
            // Example value: <bundlesPath>/plugins/java/some-plugins/
            final Path parentOfPluginDir = pluginDirectory.getParent();
            logger.debug("#uninstall() key{}-{}. Is Exists? dir:{}, file:{}", pluginKey, pluginVersion, Files.exists(pluginDirectory), Files.exists(pluginFile));

            FilesUtils.deleteRecursively(pluginDirectory);
            logger.debug("#uninstall() Is still exists after deleteRecursively()? dir: {}, file: {}", Files.exists(pluginDirectory), Files.exists(pluginFile));

            final String symlinkName = DefaultPluginFileService.DEFAULT_SYMLINK_NAME;
            try (final Stream<Path> stream = Files.list(parentOfPluginDir)) {
                final Set<Path> parentOfPluginDirSet = stream.collect(Collectors.toUnmodifiableSet());

                // Delete SET_DEFAULT directory (will re-set later if the same pluginKey exist)
                parentOfPluginDirSet.stream()
                        .filter(path -> path.getFileName().toString().equals(symlinkName))
                        .forEach(FilesUtils::deleteIfExists);

                final Path nextLatestPluginForKey = parentOfPluginDirSet.stream()
                        .filter(path -> !path.getFileName().toString().equals(symlinkName))
                        .max(Comparator.naturalOrder())
                        .orElse(null);
                logger.debug("nextLatestPluginForKey: {}", nextLatestPluginForKey);
                if (nextLatestPluginForKey != null) {
                    pluginFileService.createSymlink(nextLatestPluginForKey);
                    return nextLatestPluginForKey;
                } else {
                    // Removed plugin is the last plugin by key. Delete the pluginKey directory
                    FilesUtils.deleteIfExists(parentOfPluginDir);
                }
            }
        } catch (final IOException e) {
            final String msg = String.format("Cannot uninstall: %s version: %s, because %s", pluginKey, pluginVersion, e.getMessage());
            throw new KPMPluginException(msg, e);
        }
        return null;
    }
}
