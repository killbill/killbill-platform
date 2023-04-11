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
import java.util.Properties;

import javax.annotation.Nonnull;

import org.killbill.billing.osgi.bundles.kpm.KpmProperties;
import org.killbill.billing.osgi.bundles.kpm.PluginFileService;
import org.killbill.commons.utils.annotation.VisibleForTesting;

public class DefaultPluginFileService implements PluginFileService {

    @VisibleForTesting
    static final String DEFAULT_SYMLINK_NAME = "SET_DEFAULT";

    private final Path bundlesPath;

    public DefaultPluginFileService(final KpmProperties kpmProperties) {
        this.bundlesPath = kpmProperties.getBundlesPath();
    }

    @Override
    public Path createPluginDirectory(final String pluginKey, final String pluginVersion) throws IOException {
        final PluginNamingResolver pluginNamingResolver = PluginNamingResolver.of(pluginKey, pluginVersion);
        final String pluginName = pluginNamingResolver.getPluginName();
        final String fixedVersion = pluginNamingResolver.getPluginVersion();
        final Path pluginDirectory = Path.of(bundlesPath.toString(), "plugins", "java", pluginName, fixedVersion);
        return Files.createDirectories(pluginDirectory);
    }

    @Override
    public void createSymlink(@Nonnull final Path pluginDirectory) throws IOException {
        final Path symlink = pluginDirectory.resolveSibling(DEFAULT_SYMLINK_NAME);
        final Path parentDir = symlink.getParent();
        // (null check required by spotbugs) Files.createSymbolicLink(path) mandates that parent directory should exist ....
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        // .... BUT symbolic link directory should not.
        Files.deleteIfExists(symlink);
        Files.createSymbolicLink(symlink, pluginDirectory);
    }
}
