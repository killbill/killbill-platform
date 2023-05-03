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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;

import org.killbill.billing.osgi.bundles.kpm.impl.FilesUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestKPMClient {

    private static final String PLUGIN_DIR = "https://raw.githubusercontent.com/killbill/killbill-cloud/master/kpm/lib/kpm/plugins_directory.yml";
    private static final String GITHUB_URL = "https://maven.pkg.github.com/killbill/qualpay-java-client/org/kill-bill/billing/thirdparty/qualpay-java-client/1.1.9/qualpay-java-client-1.1.9.pom";
    private static final String CLOUDSMITH_URL = "https://dl.cloudsmith.io/basic/some-org/testing/maven/org/kill-bill/billing/plugin/java/hello-world-plugin/2.0.1-SNAPSHOT/hello-world-plugin-2.0.1-20230412.141435-1.jar";
    private static final String CLOUDSMITH_TOKEN_URL = "https://dl.cloudsmith.io/%s/killbill/%s/raw/files/maven-metadata.xml";

    private KPMClient kpmClient;

    @BeforeMethod(groups = "slow")
    public void beforeMethod() throws GeneralSecurityException {
        kpmClient = new KPMClient(false, 60000, 60000);
    }

    private Map<String, String> basicAuth(final String plainCredentials) {
        final String encoded = Base64.getEncoder().encodeToString(plainCredentials.getBytes(StandardCharsets.UTF_8));
        return Map.of("Authorization", "Basic " + encoded);
    }

    private Map<String, String> tokenAuth(final String token) {
        return Map.of("Authorization", "token " + token);
    }

    @Test(groups = "slow")
    public void testPluginsDirectory() {
        Path result = null;
        try {
            result = kpmClient.downloadToTempOS(PLUGIN_DIR, "test-plugins_directory", ".yml");
            Assert.assertTrue(Files.size(result) > 0);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } finally {
            FilesUtils.deleteIfExists(result);
        }
    }
    @Test(groups = "slow", enabled = false, description = "set 'plainCreds' properly before running")
    public void testGithub() {
        final String plainCreds = "<github-username>:<github-key-whatever>";
        Path result = null;
        try {
            result = kpmClient.downloadToTempOS(GITHUB_URL, basicAuth(plainCreds), "test-github", ".jar");
            Assert.assertTrue(Files.size(result) > 0);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            FilesUtils.deleteIfExists(result);
        }
    }

    @Test(groups = "slow", enabled = false, description = "set 'plainCreds' properly before running")
    public void testCloudsmith() {
        final String plainCreds = "<cloudsmith-username>:<password>";
        Path result = null;
        try {
            result = kpmClient.downloadToTempOS(CLOUDSMITH_URL, basicAuth(plainCreds), "test-cloudsmith", ".jar");
            Assert.assertTrue(Files.size(result) > 0);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            FilesUtils.deleteIfExists(result);
        }
    }

    @Test(groups = "slow", enabled = false, description = "set 'token' and 'userSpecificsToken' properly before running")
    public void testCloudsmithWithToken() {
        final String token = "<cloudsmith-user-api-key>";
        final String userSpecificsToken = "<your-cloudsmith-url-key>";
        final String repositoryName = "<your-repository-name>";
        Path result = null;
        try {
            result = kpmClient.downloadToTempOS(String.format(CLOUDSMITH_TOKEN_URL, userSpecificsToken, repositoryName),
                                                tokenAuth(token),
                                                "maven-metadata",
                                                ".xml");
            final String content = Files.readString(result);
            Assert.assertFalse(content.isEmpty());
            Assert.assertTrue(content.contains("latest"));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            FilesUtils.deleteIfExists(result);
        }
    }
}
