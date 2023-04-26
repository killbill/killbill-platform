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

package org.killbill.billing.osgi.bundles.kpm;

import java.nio.file.Path;

public interface PluginInstaller {

    /**
     * Copy plugin JAR file from {@code downloadedFile} parameter to Kill Bill's {@bundlePath} directory (Kill Bill
     * configuration property named {@code org.killbill.osgi.bundle.install.dir})
     *
     * @return path where the plugin installed.
     * @throws KPMPluginException if something happened during installation
     */
    Path install(final Path downloadedFile, final String pluginKey, final String pluginVersion) throws KPMPluginException;

    /**
     * Remove plugin file from Kill Bill's {@code bundlePath} directory.
     *
     * @return latest plugin file if current {@code pluginKey} contains more than 1 plugin JAR (with different version),
     *         or {@code null} if the removed plugin is the last plugin for that {@code pluginKey}
     * @throws KPMPluginException if any error happened during removing plugin file from Kill Bill {@code bundlePath}
     */
    Path uninstall(final String pluginKey, final String pluginVersion) throws KPMPluginException;
}
