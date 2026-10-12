/*
 * Copyright 2020-2026 Equinix, Inc
 * Copyright 2014-2026 The Billing Project, LLC
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

package org.killbill.billing.osgi.bundles.logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.jetty.Jetty;
import org.jooby.json.Jackson;
import org.killbill.billing.ObjectType;
import org.killbill.billing.util.api.RecordIdApi;
import org.osgi.service.log.LogService;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class TestLogsSseHandler {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final Long ACCOUNT_RECORD_ID = 42L;

    private Jooby app;
    private LogEntriesManager logEntriesManager;
    private LogsSseHandler logsSseHandler;
    private RecordIdApi recordIdApi;
    private int port;

    @BeforeMethod(groups = "slow")
    public void setUp() throws Exception {
        port = findFreePort();
        logEntriesManager = new LogEntriesManager();
        recordIdApi = Mockito.mock(RecordIdApi.class);
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(ACCOUNT_ID), Mockito.eq(ObjectType.ACCOUNT), Mockito.any()))
               .thenReturn(ACCOUNT_RECORD_ID);
        logsSseHandler = new LogsSseHandler(logEntriesManager, recordIdApi);

        final Config testConfig = ConfigFactory.empty("test-config")
                .withValue("server.join", ConfigValueFactory.fromAnyRef(false))
                .withValue("application.port", ConfigValueFactory.fromAnyRef(port))
                .withValue("server.module", ConfigValueFactory.fromAnyRef(Jetty.class.getName()));

        app = new Jooby() {{
            use(new Jackson());
            sse("/", logsSseHandler);
        }};

        app.use(new Jooby.Module() {
            @Override
            public void configure(final Env env, final Config config, final Binder binder) {
            }

            @Override
            public Config config() {
                return testConfig;
            }
        });

        app.start();
    }

    @AfterMethod(groups = "slow")
    public void tearDown() throws Exception {
        if (logsSseHandler != null) {
            logsSseHandler.close();
        }
        if (app != null) {
            app.stop();
        }
    }

    @Test(groups = "slow")
    public void testReceivesLogEntriesBeforeConnect() throws Exception {
        recordErrorEvent("com.acme.MyService", "Something broke", "user-token-123", "1", "2");
        recordInfoEvent("com.acme.OtherService", "All good here", null, null, null);

        final HttpResponse<InputStream> response = connectSse();
        final ExecutorService reader = Executors.newSingleThreadExecutor();
        try {
            final Future<String> future = reader.submit(() -> {
                final StringBuilder sb = new StringBuilder();
                try (final BufferedReader br = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append('\n');
                        if (sb.toString().contains("Something broke") && sb.toString().contains("All good here")) {
                            break;
                        }
                    }
                }
                return sb.toString();
            });

            final String sseOutput = future.get(10, TimeUnit.SECONDS);

            Assert.assertTrue(sseOutput.contains("Something broke"), "Expected 'Something broke' in SSE output");
            Assert.assertTrue(sseOutput.contains("All good here"), "Expected 'All good here' in SSE output");
            Assert.assertTrue(sseOutput.contains("ERROR"), "Expected 'ERROR' level in SSE output");
            Assert.assertTrue(sseOutput.contains("INFO"), "Expected 'INFO' level in SSE output");
            Assert.assertTrue(sseOutput.contains("user-token-123"), "Expected userToken in SSE output");
            Assert.assertFalse(sseOutput.contains("I never recorded in event"));
        } finally {
            reader.shutdownNow();
            response.body().close();
        }
    }

    @Test(groups = "slow")
    public void testReceivesLiveLogEntries() throws Exception {
        final HttpResponse<InputStream> response = connectSse();
        Thread.sleep(1000); // wait for SSE subscription to be established

        recordWarningEvent("com.acme.LiveService", "Live event arrived", null, null, null);

        final ExecutorService reader = Executors.newSingleThreadExecutor();
        try {
            final Future<String> future = reader.submit(() -> {
                final StringBuilder sb = new StringBuilder();
                try (final BufferedReader br = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append('\n');
                        if (sb.toString().contains("Live event arrived")) {
                            break;
                        }
                    }
                }
                return sb.toString();
            });

            final String sseOutput = future.get(10, TimeUnit.SECONDS);

            Assert.assertTrue(sseOutput.contains("Live event arrived"), "Expected live event in SSE output");
            Assert.assertTrue(sseOutput.contains("WARNING"), "Expected 'WARNING' level in SSE output");
        } finally {
            reader.shutdownNow();
            response.body().close();
        }
    }

    @Test(groups = "slow")
    public void testFilterByAccountId() throws Exception {
        recordErrorEvent("com.acme.ServiceA", "Matching account event", null, null, ACCOUNT_RECORD_ID.toString());
        recordErrorEvent("com.acme.ServiceB", "Other account event", null, null, "999");
        recordInfoEvent("com.acme.ServiceC", "No account event", null, null, null);

        final HttpResponse<InputStream> response = connectSse(Map.of("accountId", ACCOUNT_ID.toString()));
        final ExecutorService reader = Executors.newSingleThreadExecutor();
        try {
            final Future<String> future = reader.submit(() -> {
                final StringBuilder sb = new StringBuilder();
                try (final BufferedReader br = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append('\n');
                        if (sb.toString().contains("Matching account event")) {
                            break;
                        }
                    }
                }
                return sb.toString();
            });

            final String sseOutput = future.get(10, TimeUnit.SECONDS);

            Assert.assertTrue(sseOutput.contains("Matching account event"));
            Assert.assertFalse(sseOutput.contains("Other account event"));
            Assert.assertFalse(sseOutput.contains("No account event"));
        } finally {
            reader.shutdownNow();
            response.body().close();
        }
    }

    @Test(groups = "slow")
    public void testFilterByUserToken() throws Exception {
        recordErrorEvent("com.acme.ServiceA", "Token match event", "my-token", null, null);
        recordErrorEvent("com.acme.ServiceB", "Other token event", "other-token", null, null);
        recordInfoEvent("com.acme.ServiceC", "No token event", null, null, null);

        final HttpResponse<InputStream> response = connectSse(Map.of("userToken", "my-token"));
        final ExecutorService reader = Executors.newSingleThreadExecutor();
        try {
            final Future<String> future = reader.submit(() -> {
                final StringBuilder sb = new StringBuilder();
                try (final BufferedReader br = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append('\n');
                        if (sb.toString().contains("Token match event")) {
                            break;
                        }
                    }
                }
                return sb.toString();
            });

            final String sseOutput = future.get(10, TimeUnit.SECONDS);

            Assert.assertTrue(sseOutput.contains("Token match event"));
            Assert.assertFalse(sseOutput.contains("Other token event"));
            Assert.assertFalse(sseOutput.contains("No token event"));
        } finally {
            reader.shutdownNow();
            response.body().close();
        }
    }

    @Test(groups = "slow")
    public void testFilterByAccountIdOrUserToken() throws Exception {
        // OR logic: entry passes if it matches accountId OR userToken
        recordErrorEvent("com.acme.ServiceA", "Account match", null, null, ACCOUNT_RECORD_ID.toString());
        recordErrorEvent("com.acme.ServiceB", "Token match", "my-token", null, "999");
        recordInfoEvent("com.acme.ServiceC", "Neither match", "other-token", null, "888");

        final HttpResponse<InputStream> response = connectSse(Map.of(
                "accountId", ACCOUNT_ID.toString(),
                "userToken", "my-token"));
        final ExecutorService reader = Executors.newSingleThreadExecutor();
        try {
            final Future<String> future = reader.submit(() -> {
                final StringBuilder sb = new StringBuilder();
                try (final BufferedReader br = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append('\n');
                        if (sb.toString().contains("Account match") && sb.toString().contains("Token match")) {
                            break;
                        }
                    }
                }
                return sb.toString();
            });

            final String sseOutput = future.get(10, TimeUnit.SECONDS);

            Assert.assertTrue(sseOutput.contains("Account match"), "Expected account-matched entry (OR)");
            Assert.assertTrue(sseOutput.contains("Token match"), "Expected token-matched entry (OR)");
            Assert.assertFalse(sseOutput.contains("Neither match"), "Should not contain unrelated entry");
        } finally {
            reader.shutdownNow();
            response.body().close();
        }
    }

    @Test(groups = "slow")
    public void testHeartbeatSentWhenAllEntriesFilteredOut() throws Exception {
        // Record entries that will NOT match the filter
        recordErrorEvent("com.acme.ServiceA", "Unrelated event", "other-token", null, "999");
        recordInfoEvent("com.acme.ServiceB", "Another unrelated", null, null, null);

        final HttpResponse<InputStream> response = connectSse(Map.of("userToken", "non-existent-token"));
        final ExecutorService reader = Executors.newSingleThreadExecutor();
        try {
            final Future<String> future = reader.submit(() -> {
                final StringBuilder sb = new StringBuilder();
                try (final BufferedReader br = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append('\n');
                        if (sb.toString().contains("heartbeat")) {
                            break;
                        }
                    }
                }
                return sb.toString();
            });

            final String sseOutput = future.get(10, TimeUnit.SECONDS);

            Assert.assertTrue(sseOutput.contains("heartbeat"), "Expected heartbeat when no entries match filter");
            Assert.assertFalse(sseOutput.contains("Unrelated event"), "Filtered entries should not appear");
            Assert.assertFalse(sseOutput.contains("Another unrelated"), "Filtered entries should not appear");
        } finally {
            reader.shutdownNow();
            response.body().close();
        }
    }

    @Test(groups = "slow")
    public void testFullSseLifecycle() throws Exception {
        // -- Step 1-3: Connect without filter or Last-Event-ID. Read initial entries. --
        recordInfoEvent("com.acme.Init", "Entry one", null, null, null);
        recordInfoEvent("com.acme.Init", "Entry two", null, null, null);

        String lastReceivedId;
        try (final SseClientForTesting session = SseClientForTesting.connect(port)) {
            final String output = session.readUntil("Entry one", "Entry two");
            Assert.assertTrue(output.contains("Entry one"));
            Assert.assertTrue(output.contains("Entry two"));

            // Extract the last event id from the SSE stream for reconnection later
            lastReceivedId = extractLastEventId(output);
            Assert.assertNotNull(lastReceivedId, "Should have received at least one event id");
        }

        // -- Step 4: Server adds entry with userToken --
        recordErrorEvent("com.acme.Auth", "Token event", "request-abc", null, null);

        // -- Step 5: Filter by non-matching userToken -> heartbeat only --
        try (final SseClientForTesting session = SseClientForTesting.connect(port, Map.of("userToken", "non-existent"))) {
            final String output = session.readUntil("heartbeat");
            Assert.assertTrue(output.contains("heartbeat"));
            Assert.assertFalse(output.contains("Token event"));
            Assert.assertFalse(output.contains("Entry one"));
        }

        // -- Step 6: Filter by matching userToken -> match --
        try (final SseClientForTesting session = SseClientForTesting.connect(port, Map.of("userToken", "request-abc"))) {
            final String output = session.readUntil("Token event");
            Assert.assertTrue(output.contains("Token event"));
            Assert.assertFalse(output.contains("Entry one"), "Non-matching entries should be filtered");
        }

        // -- Step 7: Server adds entry with accountRecordId --
        recordWarningEvent("com.acme.Billing", "Account event", null, null, ACCOUNT_RECORD_ID.toString());

        // -- Step 8a: Filter by non-matching accountId -> heartbeat only --
        final UUID wrongAccountId = UUID.randomUUID();
        try (final SseClientForTesting session = SseClientForTesting.connect(port, Map.of("accountId", wrongAccountId.toString()))) {
            final String output = session.readUntil("heartbeat");
            Assert.assertTrue(output.contains("heartbeat"));
            Assert.assertFalse(output.contains("Account event"));
        }

        // -- Step 8b: Filter by matching accountId -> match --
        try (final SseClientForTesting session = SseClientForTesting.connect(port, Map.of("accountId", ACCOUNT_ID.toString()))) {
            final String output = session.readUntil("Account event");
            Assert.assertTrue(output.contains("Account event"));
            Assert.assertFalse(output.contains("Entry one"), "Non-matching entries should be filtered");
        }

        // -- Step 9: Filter with both userToken AND accountId (OR logic) -> either match passes --
        try (final SseClientForTesting session = SseClientForTesting.connect(port, Map.of(
                "accountId", ACCOUNT_ID.toString(),
                "userToken", "request-abc"))) {
            final String output = session.readUntil("Token event", "Account event");
            Assert.assertTrue(output.contains("Token event"), "userToken-matched entry should pass (OR)");
            Assert.assertTrue(output.contains("Account event"), "accountRecordId-matched entry should pass (OR)");
            Assert.assertFalse(output.contains("Entry one"), "Non-matching entries should be filtered");
        }

        // -- Step 10: Filter with no match -> heartbeat only --
        try (final SseClientForTesting session = SseClientForTesting.connect(port, Map.of("userToken", "totally-unknown"))) {
            final String output = session.readUntil("heartbeat");
            Assert.assertTrue(output.contains("heartbeat"));
            Assert.assertFalse(output.contains("Token event"));
            Assert.assertFalse(output.contains("Account event"));
            Assert.assertFalse(output.contains("Entry one"));
        }

        // -- Step 11: Server adds more entries --
        recordInfoEvent("com.acme.New", "Fresh entry after resume point", null, null, null);

        // -- Step 12: Reconnect with Last-Event-ID (no filter) -> only entries after that ID --
        try (final SseClientForTesting session = SseClientForTesting.connect(port, Map.of(), lastReceivedId)) {
            final String output = session.readUntil("Fresh entry after resume point");
            Assert.assertTrue(output.contains("Fresh entry after resume point"), "New entry should be received");
            Assert.assertTrue(output.contains("Token event"), "Entry added after resume point should be received");
            Assert.assertTrue(output.contains("Account event"), "Entry added after resume point should be received");
            Assert.assertFalse(output.contains("Entry one"), "Entry before Last-Event-ID should not be received");
            Assert.assertFalse(output.contains("Entry two"), "Entry before Last-Event-ID should not be received");
        }
    }

    // -- Helpers --

    private void recordInfoEvent(final String loggerName,
                                    final String message,
                                    final String userToken,
                                    final String tenantRecordId,
                                    final String accountRecordId) {
        recordEvent(LogService.LOG_INFO, loggerName, message, userToken, tenantRecordId, accountRecordId);
    }

    private void recordWarningEvent(final String loggerName,
                                    final String message,
                                    final String userToken,
                                    final String tenantRecordId,
                                    final String accountRecordId) {
        recordEvent(LogService.LOG_WARNING, loggerName, message, userToken, tenantRecordId, accountRecordId);
    }

    private void recordErrorEvent(final String loggerName,
                                    final String message,
                                    final String userToken,
                                    final String tenantRecordId,
                                    final String accountRecordId) {
        recordEvent(LogService.LOG_ERROR, loggerName, message, userToken, tenantRecordId, accountRecordId);
    }

    private void recordEvent(final int level,
                             final String loggerName,
                             final String message,
                             final String userToken,
                             final String tenantRecordId,
                             final String accountRecordId) {
        logEntriesManager.recordEvent(new LogEntryJson(null, level, loggerName, message, userToken, tenantRecordId, accountRecordId, null));
    }

    private HttpResponse<InputStream> connectSse(final Map<String, String> queryParams) throws Exception {
        final StringBuilder uriBuilder = new StringBuilder("http://localhost:").append(port).append("/");
        if (!queryParams.isEmpty()) {
            final String query = queryParams.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
            uriBuilder.append("?").append(query);
        }

        final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        final HttpRequest request = HttpRequest.newBuilder(URI.create(uriBuilder.toString()))
                .header("Accept", "text/event-stream")
                .GET()
                .build();

        final HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        Assert.assertEquals(response.statusCode(), 200);
        return response;
    }

    private HttpResponse<InputStream> connectSse() throws Exception {
        return connectSse(Map.of());
    }

    private static int findFreePort() throws Exception {
        try (final ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * Extracts the last "id:" value from raw SSE output.
     * SSE format: "id:some-uuid\n"
     */
    private static String extractLastEventId(final String sseOutput) {
        String lastId = null;
        for (final String line : sseOutput.split("\n")) {
            if (line.startsWith("id:")) {
                lastId = line.substring(3).trim();
            }
        }
        return lastId;
    }
}


/**
 * Lightweight SSE client for tests. Each instance represents one connection lifecycle:
 * connect → read → close.
 */
class SseClientForTesting implements AutoCloseable {

    private final HttpResponse<InputStream> response;
    private final ExecutorService reader;

    private SseClientForTesting(final HttpResponse<InputStream> response) {
        this.response = response;
        this.reader = Executors.newSingleThreadExecutor();
    }

    public static SseClientForTesting connect(final int port) throws Exception {
        return connect(port, Map.of(), null);
    }

    public static SseClientForTesting connect(final int port, final Map<String, String> queryParams) throws Exception {
        return connect(port, queryParams, null);
    }

    public static SseClientForTesting connect(final int port,
                                              final Map<String, String> queryParams,
                                              final String lastEventId) throws Exception {
        final StringBuilder uriBuilder = new StringBuilder("http://localhost:").append(port).append("/");
        if (!queryParams.isEmpty()) {
            final String query = queryParams.entrySet().stream()
                                            .map(e -> e.getKey() + "=" + e.getValue())
                                            .collect(Collectors.joining("&"));
            uriBuilder.append("?").append(query);
        }

        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(uriBuilder.toString()))
                                                              .header("Accept", "text/event-stream")
                                                              .GET();

        if (lastEventId != null) {
            requestBuilder.header("Last-Event-ID", lastEventId);
        }

        final HttpClient client = HttpClient.newBuilder()
                                            .connectTimeout(Duration.ofSeconds(5))
                                            .build();

        final HttpResponse<InputStream> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("SSE connection failed with status " + response.statusCode());
        }

        return new SseClientForTesting(response);
    }

    /**
     * Reads the SSE stream until all expected strings are found, or times out.
     */
    public String readUntil(final String... expectedContents) throws Exception {
        return readUntil(10, expectedContents);
    }

    /**
     * Reads the SSE stream until all expected strings are found, or times out.
     */
    public String readUntil(final int timeoutSeconds, final String... expectedContents) throws Exception {
        final Future<String> future = reader.submit(() -> {
            final StringBuilder sb = new StringBuilder();
            try (final BufferedReader br = new BufferedReader(new InputStreamReader(response.body()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                    if (containsAll(sb.toString(), expectedContents)) {
                        break;
                    }
                }
            }
            return sb.toString();
        });
        return future.get(timeoutSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void close() throws Exception {
        reader.shutdownNow();
        response.body().close();
    }

    private static boolean containsAll(final String content, final String... expected) {
        for (final String s : expected) {
            if (!content.contains(s)) {
                return false;
            }
        }
        return true;
    }
}
