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

import java.nio.file.Path;
import java.util.Collections;

import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.killbill.billing.osgi.bundles.kpm.TestUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestSha1Checker {

    @Test(groups = "fast")
    public void testDownloadedFileVerified() {
        final KPMClient httpClient = Mockito.mock(KPMClient.class);

        final Path validPlugin = TestUtils.copyTestResourceToTemp("sha1", "valid-plugin.jar.txt");
        final Path validPluginSha1 = TestUtils.copyTestResourceToTemp("sha1", "valid-plugin.jar.txt.sha1");

        Mockito.when(httpClient.downloadToTempOS("valid", Collections.emptyMap(), "plugin", ".jar.sha1")).thenReturn(validPluginSha1);

        Sha1Checker sha1Checker = new Sha1Checker(httpClient, true);
        boolean result = sha1Checker.isDownloadedFileVerified("valid", Collections.emptyMap(), validPlugin);

        Assert.assertTrue(result);

        FilesUtils.deleteIfExists(validPlugin);
        FilesUtils.deleteIfExists(validPluginSha1);

        final Path invalidPlugin = TestUtils.copyTestResourceToTemp("sha1", "invalid-plugin.jar.txt");
        final Path invalidPluginSha1 = TestUtils.copyTestResourceToTemp("sha1", "invalid-plugin.jar.txt.sha1");

        Mockito.when(httpClient.downloadToTempOS("invalid", Collections.emptyMap(), "plugin", ".jar.sha1")).thenReturn(invalidPluginSha1);

        sha1Checker = new Sha1Checker(httpClient, true);
        result = sha1Checker.isDownloadedFileVerified("invalid", Collections.emptyMap(), invalidPlugin);

        Assert.assertFalse(result);

        FilesUtils.deleteIfExists(invalidPlugin);
        FilesUtils.deleteIfExists(invalidPluginSha1);
    }
}
