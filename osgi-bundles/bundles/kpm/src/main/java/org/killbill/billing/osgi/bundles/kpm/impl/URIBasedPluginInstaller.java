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
import java.util.Objects;

import org.killbill.billing.osgi.bundles.kpm.KPMPluginException;
import org.killbill.billing.osgi.bundles.kpm.PluginFileService;
import org.killbill.billing.osgi.bundles.kpm.PluginInstaller;

public class URIBasedPluginInstaller implements PluginInstaller {

    private final PluginFileService pluginFileService;
    private final Path downloadedFile;
    private final String pluginKey;
    private final String pluginVersion;

    public URIBasedPluginInstaller(final PluginFileService pluginFileService,
                                   final Path downloadedFile,
                                   final String pluginKey,
                                   final String pluginVersion) {
        this.pluginFileService = Objects.requireNonNull(pluginFileService);
        this.downloadedFile = Objects.requireNonNull(downloadedFile);
        this.pluginKey = Objects.requireNonNull(pluginKey);
        this.pluginVersion = pluginVersion;
    }

    /**
     * See point 6 of "Current Implementation" in <a href="https://github.com/killbill/technical-support/issues/92">this issue</a>.
     */
    @Override
    public void install() throws KPMPluginException {
        try {
            // Make directory
            final Path pluginDirectory = pluginFileService.createPluginDirectory(pluginKey, pluginVersion);

            // Copy downloadedFile to directory
            Files.copy(downloadedFile,
                       pluginDirectory.resolve(PluginNamingResolver.of(pluginKey, pluginVersion).getPluginJarFileName()),
                       StandardCopyOption.REPLACE_EXISTING);

            // Make symlink
            pluginFileService.createSymlink(pluginDirectory);
        } catch (final IOException e) {
            throw new KPMPluginException(String.format("Unable to install plugin with key: %s", pluginKey), e);
        }
    }
}
