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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.killbill.billing.plugin.util.http.InvalidRequest;
import org.killbill.billing.plugin.util.http.ResponseFormat;
import org.killbill.billing.plugin.util.http.SslUtils;
import org.killbill.billing.plugin.util.http.URIUtils;
import org.killbill.billing.plugin.util.http.UTF8UrlEncoder;
import org.killbill.commons.utils.annotation.VisibleForTesting;
import org.killbill.commons.utils.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.killbill.billing.plugin.util.http.ResponseFormat.RAW;

/**
 * FIXME plugin-framework-java: <a href="https://github.com/killbill/killbill-plugin-framework-java/issues/83">See here</a>
 */
class ForkedHttpClient implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(ForkedHttpClient.class);

    protected static final String APPLICATION_JSON = "application/json";
    protected static final String APPLICATION_XML = "application/xml";
    protected static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

    protected static final String GET = "GET";
    protected static final String POST = "POST";
    protected static final String PUT = "PUT";
    protected static final String DELETE = "DELETE";
    protected static final String HEAD = "HEAD";
    protected static final String OPTIONS = "OPTIONS";

    protected static final String USER_AGENT = "KillBill/1.0";

    protected static final int DEFAULT_HTTP_TIMEOUT_SEC = 70;
    private static final int DEFAULT_HTTP_CONNECT_TIMEOUT_SEC = 5;

    protected final String url;
    protected final java.net.http.HttpClient httpClient;
    protected final ObjectMapper mapper;

    protected final String username;
    protected final String password;

    protected int httpTimeoutSec = DEFAULT_HTTP_TIMEOUT_SEC;

    public ForkedHttpClient(final String url,
                            final String username,
                            final String password,
                            final String proxyHost,
                            final Integer proxyPort,
                            final boolean strictSSL) throws GeneralSecurityException {
        this.url = url;
        this.username = username;
        this.password = password;
        this.httpClient = buildHttpClient(strictSSL, DEFAULT_HTTP_CONNECT_TIMEOUT_SEC * 1000, proxyHost, proxyPort);
        this.mapper = createObjectMapper();
    }

    public ForkedHttpClient(final String url,
                            final String username,
                            final String password,
                            final String proxyHost,
                            final Integer proxyPort,
                            final boolean strictSSL,
                            final int connectTimeoutMs) throws GeneralSecurityException {
        this.url = url;
        this.username = username;
        this.password = password;
        this.httpClient = buildHttpClient(strictSSL, connectTimeoutMs, proxyHost, proxyPort);
        this.mapper = createObjectMapper();
    }

    public ForkedHttpClient(final String url,
                            final String username,
                            final String password,
                            final String proxyHost,
                            final Integer proxyPort,
                            final boolean strictSSL,
                            final int connectTimeoutMs,
                            final int requestTimeoutMs) throws GeneralSecurityException {
        this.url = url;
        this.username = username;
        this.password = password;
        this.httpClient = buildHttpClient(strictSSL, connectTimeoutMs, proxyHost, proxyPort);
        this.mapper = createObjectMapper();
        this.httpTimeoutSec = requestTimeoutMs;
    }

    private java.net.http.HttpClient buildHttpClient(final boolean strictSSL,
                                                     final int connectTimeoutMs,
                                                     @Nullable final String proxyHost,
                                                     @Nullable final Integer proxyPort) throws GeneralSecurityException {
        final java.net.http.HttpClient.Builder builder = java.net.http.HttpClient.newBuilder()
                                                                                 .sslContext(SslUtils.getInstance().getSSLContext(!strictSSL))
                                                                                 .connectTimeout(Duration.of(connectTimeoutMs, ChronoUnit.MILLIS))
                                                                                 .followRedirects(Redirect.NORMAL);

        if (proxyHost != null && proxyPort != null) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));
        }

        return builder.build();
    }

    @Override
    public void close() {
    }

    protected ObjectMapper createObjectMapper() {
        return JsonMapper.builder()
                         // Allow special characters
                         .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                         // Write dates using a ISO-8601 compliant notation
                         .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                         // Tells the serializer to only include those parameters that are not null
                         .serializationInclusion(JsonInclude.Include.NON_NULL)
                         .build();
    }

    protected String doCallAndReturnTextResponse(final String verb,
                                                 final String uri,
                                                 final String body,
                                                 final Map<String, String> queryParams,
                                                 final Map<String, String> headers) throws InvalidRequest,
                                                                                           InterruptedException,
                                                                                           IOException,
                                                                                           URISyntaxException {
        return doCall(verb, uri, body, queryParams, headers, String.class, ResponseFormat.TEXT);
    }

    protected <T> T doCall(final String verb,
                           final String url,
                           final String body,
                           final Map<String, String> queryParams,
                           final Map<String, String> headers,
                           final Class<T> clazz,
                           final ResponseFormat format) throws InterruptedException, IOException, URISyntaxException, InvalidRequest {
        final java.net.http.HttpRequest.Builder builder = getBuilderWithHeaderAndQuery(verb, url, headers, queryParams);
        if (!GET.equals(verb) && !HEAD.equals(verb)) {
            if (body != null) {
                builder.method(verb, BodyPublishers.ofString(body));
            }
        }

        return executeAndWait(builder, httpTimeoutSec, clazz, format);
    }

    // Logging can be enabled vi -Djdk.httpclient.HttpClient.log=errors,requests,headers,frames[:control:data:window:all..],content,ssl,trace,channel
    protected <T> T executeAndWait(final java.net.http.HttpRequest.Builder builder,
                                   final int timeoutSec,
                                   final Class<T> clazz,
                                   final ResponseFormat format) throws IOException, InterruptedException, InvalidRequest {
        builder.timeout(Duration.of(timeoutSec, ChronoUnit.SECONDS));

        final HttpResponse<InputStream> response = httpClient.send(builder.build(), BodyHandlers.ofInputStream());

        logger.debug("ForkedHttpClient response:{}, status code:{}", response, response == null ? "response-is-null" : response.statusCode());
        if (response != null && response.statusCode() == 401) {
            throw new InvalidRequest("Unauthorized request", response);
        } else if (response != null && response.statusCode() >= 400) {
            throw new InvalidRequest("Invalid request", response);
        } else if (response == null) {
            throw new InvalidRequest("No response");
        }

        return deserializeResponse(response, clazz, format);
    }

    protected <T> T deserializeResponse(final HttpResponse<InputStream> response, final Class<T> clazz, final ResponseFormat format) throws IOException {
        if (format == RAW) {
            // Don't close the stream!
            return (T) response.body();
        }

        try (final InputStream in = response.body()) {
            switch (format) {
                case TEXT:
                    return (T) CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
                default:
                    return mapper.readValue(in, clazz);
            }
        }
    }

    protected java.net.http.HttpRequest.Builder getBuilderWithHeaderAndQuery(final String verb,
                                                                             final String url,
                                                                             final Map<String, String> headers,
                                                                             final Map<String, String> queryParams) throws URISyntaxException {
        final java.net.http.HttpRequest.Builder builder = HttpRequest.newBuilder()
                                                                     .uri(getURI(url, queryParams))
                                                                     .method(verb, BodyPublishers.noBody()); // Body overridden later on

        builder.header("User-Agent", USER_AGENT);

        if (username != null && password != null) {
            // Force authentication, regardless if we were challenged
            // Note: on JDK-17, this header won't be set if a PasswordAuthentication is set, see jdk.internal.net.http.common.Utils.CONTEXT_RESTRICTED.
            // On JDK-11, it worked because of JDK-8263442 (https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8263442).
            builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
        }

        for (final Entry<String, String> entry : headers.entrySet()) {
            builder.headers(entry.getKey(), entry.getValue());
        }

        return builder;
    }

    @VisibleForTesting
    URI getURI(final String url, final Map<String, String> queryParams) throws URISyntaxException {
        if (url == null) {
            throw new URISyntaxException("(null)", "HttpClient URL misconfigured");
        }

        URI u = new URI(url);
        if (!u.isAbsolute()) {
            u = new URI(String.format("%s%s", this.url, url));
        }

        if (queryParams.isEmpty()) {
            return u;
        }

        final StringBuilder sb = new StringBuilder(u.getQuery() == null ? "" : u.getQuery());
        queryParams.keySet().forEach(name -> {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(UTF8UrlEncoder.encode(name));
            sb.append('=');
            sb.append(UTF8UrlEncoder.encode(queryParams.get(name)));
        });

        final String query = sb.toString();

        return new URI(URIUtils.buildURI(u.getScheme(),
                                         u.getUserInfo(),
                                         u.getHost(),
                                         u.getPort(),
                                         u.getAuthority(),
                                         u.getRawPath(), // Keep the raw path (don't decode it)
                                         query,
                                         u.getFragment()));
    }
}
