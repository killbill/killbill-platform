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
import java.util.Properties;

import org.killbill.billing.osgi.bundles.kpm.impl.DefaultPluginFileService;
import org.killbill.commons.utils.io.Resources;

public class TestUtils {

    /**
     * Get maven's "src/test/resources" directory path of this maven module.
     */
    public static Path getTestPath(final String... path) {
        return Path.of(Resources.getResource(".").getPath(), path);
    }

    public static Properties getTestProperties() {
        final Properties properties = new Properties();
        properties.setProperty(DefaultPluginFileService.BUNDLE_INSTALL_DIR, TestUtils.getTestPath().toString());
        return properties;
    }
}
