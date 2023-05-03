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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.killbill.billing.osgi.bundles.kpm.KPMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Sha1Checker {

    private final Logger logger = LoggerFactory.getLogger(Sha1Checker.class);

    private final KPMClient httpClient;
    private final boolean shouldVerify;

    public Sha1Checker(final KPMClient httpClient, final boolean shouldVerify) {
        this.httpClient = httpClient;
        this.shouldVerify = shouldVerify;
    }

    boolean isDownloadedFileVerified(final String sha1Url, final Map<String, String> requestHeaders, final Path downloadedPath) {
        if (!shouldVerify) {
            return true;
        }

        try (final InputStream downloadedPathStream = new FileInputStream(downloadedPath.toFile())) {
            final String downloadedPathSha1 = DigestUtils.sha1Hex(downloadedPathStream);
            final String remoteSha1 = getOrLoadOriginalSha1(sha1Url, requestHeaders);
            logger.debug("downloadedPathSha1: {}, remoteSha1: {}", downloadedPathSha1, remoteSha1);

            return downloadedPathSha1.equals(remoteSha1);
        } catch (final IOException e) {
            logger.error("SHA1 verification will just return 'false' because it throws an IOException: ", e);
            return false;
        }
    }

    private String getOrLoadOriginalSha1(final String sha1Url, final Map<String, String> requestHeader) {
        Path result = null;
        try {
            result = httpClient.downloadToTempOS(sha1Url, requestHeader, "plugin", ".jar.sha1");
            return Files.readString(result);
        } catch (final Exception ignored) {
        } finally {
            FilesUtils.deleteIfExists(result);
        }

        return "";
    }
}
