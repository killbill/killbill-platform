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

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FilesUtilsTest {

    private static final Path ACTUAL_SYMLINK_PATH;

    static {
        try {
            ACTUAL_SYMLINK_PATH = Files.createTempDirectory("test-file-utils-not-deleted");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path dirToDelete;

    @BeforeMethod(groups = "fast")
    public void beforeMethod() throws IOException {
        dirToDelete = Files.createTempDirectory("test-file-utils");
        Files.createDirectories(dirToDelete.resolve("file"));
        Files.createDirectories(dirToDelete.resolve("util"));
        Files.createFile(dirToDelete.resolve("util").resolve("file.txt"));
        Files.createSymbolicLink(dirToDelete.resolve("link"), ACTUAL_SYMLINK_PATH);

        Assert.assertTrue(Files.exists(ACTUAL_SYMLINK_PATH));

        Assert.assertTrue(Files.exists(dirToDelete));
        Assert.assertTrue(Files.exists(dirToDelete.resolve("file")));
        Assert.assertTrue(Files.exists(dirToDelete.resolve("util")));
        Assert.assertTrue(Files.exists(dirToDelete.resolve("util").resolve("file.txt")));
        Assert.assertTrue(Files.exists(dirToDelete.resolve("link")));
    }

    @AfterMethod(groups = "fast")
    public void afterMethod() {
        FilesUtils.deleteIfExists(ACTUAL_SYMLINK_PATH);
    }

    @Test(groups = "fast")
    public void testDeleteRecursively() {
        FilesUtils.deleteRecursively(dirToDelete);

        Assert.assertFalse(Files.exists(dirToDelete));
        Assert.assertTrue(Files.exists(ACTUAL_SYMLINK_PATH));
    }
}
