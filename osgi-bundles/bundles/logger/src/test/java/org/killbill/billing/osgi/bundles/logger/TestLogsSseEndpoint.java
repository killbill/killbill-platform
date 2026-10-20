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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.killbill.billing.ObjectType;
import org.killbill.billing.plugin.core.resources.jooby.PluginApp;
import org.killbill.billing.plugin.core.resources.jooby.PluginAppBuilder;
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.billing.util.api.RecordIdApi;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;

// Real Jetty + Jooby servlet + LogsSseFilter + LogsSseHandler. Only the tenant lookup is simulated:
// a servlet filter plays the role of Kill Bill's TenantFilter and sets the "killbill_tenant" attribute for the valid API key.
public class TestLogsSseEndpoint {

    private static final String VALID_API_KEY = "good-key";
    private static final int READ_TIMEOUT_MS = 5000;
    private static final long DEADLINE_NANOS = 10_000_000_000L;
    private static final String BASE_PATH = "/plugins/" + Activator.PLUGIN_NAME + "/";

    private final UUID tenantId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final UUID otherAccountId = UUID.randomUUID();

    private Server server;
    private LogsSseHandler logsSseHandler;
    private int port;

    @BeforeClass(groups = "slow")
    public void startServer() throws Exception {
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(tenantId), Mockito.eq(ObjectType.TENANT), Mockito.any())).thenReturn(1L);
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(accountId), Mockito.eq(ObjectType.ACCOUNT), Mockito.any())).thenReturn(10L);
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(otherAccountId), Mockito.eq(ObjectType.ACCOUNT), Mockito.any())).thenReturn(11L);

        final LogEntriesManager logEntriesManager = new LogEntriesManager();
        logEntriesManager.recordEvent(new LogEntryJson(null, 3, "logger", "tenant1-account10", "tok-a", "1", "10", null));
        logEntriesManager.recordEvent(new LogEntryJson(null, 3, "logger", "tenant1-account11", "tok-b", "1", "11", null));
        logEntriesManager.recordEvent(new LogEntryJson(null, 3, "logger", "tenant2-account10", "tok-a", "2", "10", null));
        logEntriesManager.recordEvent(new LogEntryJson(null, 3, "logger", "no-tenant", null, null, null, null));

        logsSseHandler = new LogsSseHandler(logEntriesManager);
        final PluginApp pluginApp = new PluginAppBuilder(Activator.PLUGIN_NAME).build();
        pluginApp.use("*", new LogsSseFilter(recordIdApi, true));
        pluginApp.sse("/", logsSseHandler);

        final Tenant tenant = Mockito.mock(Tenant.class);
        Mockito.when(tenant.getId()).thenReturn(tenantId);
        final Filter tenantFilter = (request, response, chain) -> {
            if (VALID_API_KEY.equals(((HttpServletRequest) request).getHeader(LogsSseFilter.API_KEY_HEADER))) {
                request.setAttribute(LogsSseFilter.TENANT_ATTRIBUTE, tenant);
            }
            chain.doFilter(request, response);
        };

        server = new Server();
        final ServerConnector connector = new ServerConnector(server);
        server.addConnector(connector);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.addFilter(new FilterHolder(tenantFilter), "/*", java.util.EnumSet.of(DispatcherType.REQUEST));
        final ServletHolder servlet = new ServletHolder(PluginApp.createServlet(pluginApp));
        servlet.setAsyncSupported(true);
        context.addServlet(servlet, "/*");
        server.setHandler(context);
        server.start();
        port = connector.getLocalPort();
    }

    @AfterClass(groups = "slow", alwaysRun = true)
    public void stopServer() throws Exception {
        try {
            if (server != null) {
                server.stop();
            }
        } finally {
            // Also shuts down the scheduler and closes the LogEntriesManager
            if (logsSseHandler != null) {
                logsSseHandler.close();
            }
        }
    }

    @Test(groups = "slow")
    public void testNoTenantNoParamsStreamsEverything() throws Exception {
        final Stream stream = open("", null);
        Assert.assertEquals(stream.status, 200);
        Assert.assertEquals(stream.awaitMessagesUntilHeartbeat(), List.of("tenant1-account10", "tenant1-account11", "tenant2-account10", "no-tenant"));
    }

    @Test(groups = "slow")
    public void testValidTenantOnlySeesItsOwnEntries() throws Exception {
        final Stream stream = open("", VALID_API_KEY);
        Assert.assertEquals(stream.status, 200);
        Assert.assertEquals(stream.awaitMessagesUntilHeartbeat(), List.of("tenant1-account10", "tenant1-account11"));
    }

    @Test(groups = "slow")
    public void testAccountIdNarrowsWithinTenant() throws Exception {
        final Stream stream = open("?accountId=" + accountId, VALID_API_KEY);
        Assert.assertEquals(stream.status, 200);
        Assert.assertEquals(stream.awaitMessagesUntilHeartbeat(), List.of("tenant1-account10"));
    }

    @Test(groups = "slow")
    public void testUserTokenAndAccountIdAreAnded() throws Exception {
        final Stream stream = open("?accountId=" + otherAccountId + "&userToken=tok-a", VALID_API_KEY);
        Assert.assertEquals(stream.status, 200);
        // Account 11 has only tok-b, so nothing can match: the connection stays open and only heartbeats arrive
        Assert.assertEquals(stream.awaitHeartbeats(2), 2);
        Assert.assertTrue(stream.messages.isEmpty());
    }

    @Test(groups = "slow")
    public void testNoMatchOnlyHeartbeatsWithoutId() throws Exception {
        final Stream stream = open("?userToken=nobody", VALID_API_KEY);
        Assert.assertEquals(stream.status, 200);
        Assert.assertEquals(stream.awaitHeartbeats(2), 2);
        Assert.assertTrue(stream.messages.isEmpty());
        Assert.assertTrue(stream.ids.isEmpty(), "Heartbeat before any entry must not carry an id");
    }

    @Test(groups = "slow")
    public void testAccountIdWithoutTenantIs403() throws Exception {
        Assert.assertEquals(open("?accountId=" + accountId, null).status, 403);
    }

    @Test(groups = "slow")
    public void testApiKeyHeaderWithoutValidTenantIs401() throws Exception {
        Assert.assertEquals(open("", "bad-key").status, 401);
    }

    @Test(groups = "slow")
    public void testMalformedAccountIdIs400() throws Exception {
        Assert.assertEquals(open("?accountId=not-a-uuid", VALID_API_KEY).status, 400);
    }

    @Test(groups = "slow")
    public void testRepeatedParameterIs400() throws Exception {
        Assert.assertEquals(open("?userToken=a&userToken=b", VALID_API_KEY).status, 400);
        Assert.assertEquals(open("?accountId=" + accountId + "&accountId=" + otherAccountId, VALID_API_KEY).status, 400);
    }

    private Stream open(final String query, final String apiKey) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + BASE_PATH + query).openConnection();
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("Accept", "text/event-stream");
        if (apiKey != null) {
            conn.setRequestProperty(LogsSseFilter.API_KEY_HEADER, apiKey);
            conn.setRequestProperty(LogsSseFilter.API_SECRET_HEADER, "secret");
        }
        return new Stream(conn);
    }

    private static final class Stream {

        private final HttpURLConnection conn;
        private final int status;
        private final List<String> messages = new ArrayList<>();
        private final List<String> ids = new ArrayList<>();
        private final List<String> raw = new ArrayList<>();
        private BufferedReader reader;

        Stream(final HttpURLConnection conn) throws IOException {
            this.conn = conn;
            this.status = conn.getResponseCode();
            if (status == 200) {
                this.reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            }
        }

        // The handler sends a heartbeat only once the replayed entries are drained, so reading up to it sees every entry that would be sent
        List<String> awaitMessagesUntilHeartbeat() throws IOException {
            awaitHeartbeats(1);
            return new ArrayList<>(messages);
        }

        int awaitHeartbeats(final int expected) throws IOException {
            final long deadline = System.nanoTime() + DEADLINE_NANOS;
            int heartbeats = 0;
            try {
                String line;
                while (heartbeats < expected && System.nanoTime() < deadline && (line = reader.readLine()) != null) {
                    raw.add(line);
                    if (line.equals("data:heartbeat") || line.equals("data:\"heartbeat\"")) {
                        heartbeats++;
                    } else {
                        consume(line);
                    }
                }
                Assert.assertTrue(heartbeats >= expected, "Timed out. Heartbeats=" + heartbeats + " raw lines: " + raw);
                return heartbeats;
            } finally {
                conn.disconnect();
            }
        }

        private void consume(final String line) {
            if (line.startsWith("id:")) {
                ids.add(line.substring(3));
            } else if (line.startsWith("data:{")) {
                final String json = line.substring(5);
                final int idx = json.indexOf("\"message\":\"");
                if (idx >= 0) {
                    final int start = idx + "\"message\":\"".length();
                    messages.add(json.substring(start, json.indexOf('"', start)));
                }
            }
        }
    }
}
