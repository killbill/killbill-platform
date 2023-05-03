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
import java.nio.file.StandardCopyOption;
import java.util.Map.Entry;
import java.util.Properties;

import org.killbill.commons.utils.io.Resources;

public class TestUtils {

    /**
     * Get maven's "src/test/resources" directory path of this maven module.
     */
    public static Path getTestPath(final String... path) {
        return Path.of(Resources.getResource(".").getPath(), path);
    }

    /**
     * Copy file in {@code pathToSrcTestResourceFile} to temporary file. Needed because file deletion actually performed
     * by related classes, and thus, the file will not available anymore during testing.
     *
     * @param testResourceFile any file located in "src/test/resources".
     * @return copied temporary file
     */
    // FIXME-TS-58 : It is possible that the real problem is caused by maven's surefire/failsafe (or even intellij
    //   testing integration?) where test resources files not re-copied for each testing. Investigating this may take
    //   more time, where this solution already fixed the problem.
    public static Path copyTestResourceToTemp(final String... testResourceFile) {
        try {
            final Path source = getTestPath(testResourceFile);
            final Path target = Files.createTempFile("kpm-test", "");
            return Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Properties getTestProperties() {
        final Properties properties = new Properties();
        properties.setProperty("org.killbill.osgi.bundle.install.dir", getTestPath().toString());
        return properties;
    }

    @SafeVarargs
    public static KpmProperties getKpmProperties(final String bundleInstallDir, final Entry<String, String>... additionalProperties) {
        final Properties properties = new Properties();
        properties.setProperty("org.killbill.osgi.bundle.install.dir", bundleInstallDir);
        if (additionalProperties != null) {
            for (final Entry<String, String> entry : additionalProperties) {
                properties.setProperty(entry.getKey(), entry.getValue());
            }
        }
        return new KpmProperties(properties);
    }
}
