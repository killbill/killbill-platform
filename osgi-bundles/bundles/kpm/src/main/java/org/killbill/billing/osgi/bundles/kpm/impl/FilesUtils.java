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
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * We have one at {@code org.killbill.commons.utils.io.Files}, but upgrading that class requires multiple steps. Maybe
 * some fix-me here?
 */
public class FilesUtils {

    public static void deleteIfExists(final Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (final IOException ignored) {
        }
    }

    public static void deleteRecursively(final Path path) {
        if (path == null) {
            return;
        }

        // Comparator.reverseOrder() needed to make sure everything get deleted in correct order
        try (final Stream<Path> stream = Files.walk(path).sorted(Comparator.reverseOrder())) {
            stream.forEach(FilesUtils::deleteIfExists);
        } catch (final IOException ignored) {
        } finally {
            deleteIfExists(path);
        }
    }
}
