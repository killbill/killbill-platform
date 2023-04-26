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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nonnull;

public interface PluginFileService {

    /**
     * Get download path. This is usually temporary path where plugin file should be downloaded before processed further.
     * This is satisfies "Current Implementation" point 5 section in
     * <a href="https://github.com/killbill/technical-support/issues/92">this issue</a>.
     */
    static Path createTmpDownloadPath() throws IOException {
        return Files.createTempDirectory("kpm-" + System.currentTimeMillis()).toAbsolutePath();
    }

    /**
     * Create directory for actual plugin location. {@link Path} object returned by this method:
     * <ol>
     *     <li>Will actually exist in file system</li>
     *     <li>One of hierarchical sequence should contains correct <strong>plugin name</strong>.</li>
     *     <li>One of hierarchical sequence should contains version with public <strong>semantic versioning</strong> format.</li>
     *     <li>
     *         plugin name and version will always be in {@code <plugin-name><fs-separator><version>} format.
     *         For example {@code helloworld-plugin/3.0.0}, {@code super-plugin/4.3.1}, etc.
     *     </li>
     * </ol>
     */
    Path createPluginDirectory(String pluginKey, String pluginVersion) throws IOException;

    /**
     * Create symlink for directory. Port
     * <a href="https://github.com/killbill/killbill-cloud/blob/master/kpm/lib/kpm/plugins_manager.rb#L22">setActive</a>
     * operation.
     *
     * @return symlink {@link Path}.
     */
    void createSymlink(@Nonnull final Path pluginDirectory) throws IOException;

    Path getPluginDirByPluginKeyAndVersion(@Nonnull final String pluginKey, @Nonnull final String pluginVersion);
}
