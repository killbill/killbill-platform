/*
 * Copyright 2020-2022 Equinix, Inc
 * Copyright 2014-2022 The Billing Project, LLC
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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.Builder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

import org.killbill.billing.plugin.util.http.HttpClient;
import org.killbill.billing.plugin.util.http.InvalidRequest;
import org.killbill.billing.plugin.util.http.ResponseFormat;
import org.killbill.commons.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KPMClient extends HttpClient {

    private final Logger logger = LoggerFactory.getLogger(KPMClient.class);

    public KPMClient(final boolean strictSSL,
                     final int connectTimeoutMs,
                     final int requestTimeoutMs) throws GeneralSecurityException {
        super(null, null, null, null, null, strictSSL, connectTimeoutMs, requestTimeoutMs);
    }

    @Override
    protected Builder getBuilderWithHeaderAndQuery(final String verb, final String url, final Map<String, String> headers, final Map<String, String> queryParams) throws URISyntaxException {
        logger.debug("Building request: verb={}, url={}, headers={}, queryParams={}", verb, url, headers, queryParams);
        final Builder builder = super.getBuilderWithHeaderAndQuery(verb, url, headers, queryParams);
        builder.setHeader("User-Agent", "KillBill/kpm-plugin/1.0");

        return builder;
    }

    @Override
    public java.net.http.HttpClient.Builder httpClientBuilder(final boolean strictSSL, final int connectTimeoutMs, final String proxyHost, final Integer proxyPort) throws GeneralSecurityException {
        return super.httpClientBuilder(strictSSL, connectTimeoutMs, proxyHost, proxyPort)
                    .followRedirects(Redirect.NORMAL);
    }

    public void download(final String uri, final Path target) throws InvalidRequest, IOException, URISyntaxException, InterruptedException {
        download(uri, Collections.emptyMap(), target);
    }

    public void download(final String uri, final Map<String, String> headers, final Path target) throws InvalidRequest, IOException, URISyntaxException, InterruptedException {
        logger.info("Starting download: uri={}, target={}", uri, target);
        try (final InputStream is = doCall(GET, uri, null, Collections.emptyMap(), headers, InputStream.class, ResponseFormat.RAW)) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Download completed: uri={}, target={}", uri, target);
        } catch (final Exception e) {
            logger.error("Download failed: uri={}, target={}, error={}", uri, target, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Download a file to temporary directory with empty header.
     * @param url location of remote file
     * @param nameAndExt pair or file prefix and file extension. See {@link #downloadToTempOS(String, Map, String...)} javadoc
     *                   for more info about this parameter.
     * @return local path of downloaded file in temporary directory.
     */
    public Path downloadToTempOS(final String url, final String... nameAndExt) {
        return downloadToTempOS(url, Collections.emptyMap(), nameAndExt);
    }

    /**
     * Download a file to temporary directory.
     * @param url location of remote file
     * @param headers headers needed to access the remote file. Usually used when authentication needed.
     * @param nameAndExt pair or file prefix and file extension. The rule is as follows:
     *                   <p>1. if args is not set, then the prefix is {@code "kpm"} and file extension is {@code ".tmp"}</p>
     *                   <p>2. if args size is 1, then the prefix is the args value and file extension is {@code ".tmp"}</p>
     *                   <p>3. if args size is 2, then the prefix is the 1st args value and file extension is the 2nd args value</p>
     *                   <p>4. if args size more than 2, back to point 3, and the rest of args ignored</p>
     * @return local path of downloaded file in temporary directory.
     */
    public Path downloadToTempOS(final String url, final Map<String, String> headers, final String... nameAndExt) {
        final Path target = createTarget(nameAndExt);
        try (final InputStream is = doCall(GET, url, null, Collections.emptyMap(), headers, InputStream.class, ResponseFormat.RAW)) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (final InvalidRequest ir) {
            throw new IllegalArgumentException(String.format("GET request to '%s' throw InvalidRequest", url), ir);
        } catch (final Exception e) {
            throw new RuntimeException(String.format("Error when GET request to '%s', because: %s", url, e.getMessage()), e);
        }

        return target;
    }

    private Path createTarget(final String... names) {
        try {
            if (names == null) {
                final String fileName = "kpm";
                final String ext = ".tmp";
                return Files.createTempFile(fileName, ext);
            } else {
                if (!Strings.isNullOrEmpty(names[0]) && !Strings.isNullOrEmpty(names[1])) {
                    return Files.createTempFile(names[0], names[1]);
                } else if (!Strings.isNullOrEmpty(names[0])) {
                    return Files.createTempFile(names[0], ".tmp");
                } else {
                    return createTarget(null);
                }
            }
        } catch (final IOException ex) {
            throw new RuntimeException("Cannot create temp file for downloaded file because: " + ex.getMessage(), ex);
        }
    }
}
