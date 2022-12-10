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
import java.net.http.HttpRequest.Builder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

import org.killbill.billing.plugin.util.http.HttpClient;
import org.killbill.billing.plugin.util.http.InvalidRequest;
import org.killbill.billing.plugin.util.http.ResponseFormat;

public class KPMClient extends HttpClient {

    public KPMClient(final boolean strictSSL,
                     final int connectTimeoutMs,
                     final int requestTimeoutMs) throws GeneralSecurityException {
        super(null, null, null, null, null, strictSSL, connectTimeoutMs, requestTimeoutMs);
    }

    @Override
    protected Builder getBuilderWithHeaderAndQuery(final String verb, final String url, final Map<String, String> headers, final Map<String, String> queryParams) throws URISyntaxException {
        final Builder builder = super.getBuilderWithHeaderAndQuery(verb, url, headers, queryParams);
        builder.setHeader("User-Agent", "KillBill/kpm-plugin/1.0");
        return builder;
    }

    public void downloadPlugin(final String uri, final Path target) throws InvalidRequest, IOException, URISyntaxException, InterruptedException {
        try (final InputStream pluginStream = doCall(GET,
                                                     uri,
                                                     null,
                                                     Collections.emptyMap(),
                                                     Collections.emptyMap(),
                                                     InputStream.class,
                                                     ResponseFormat.RAW)) {
            Files.copy(pluginStream, target);
        }
    }
}
