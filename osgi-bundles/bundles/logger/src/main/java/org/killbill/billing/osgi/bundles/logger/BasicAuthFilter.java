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

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.killbill.billing.security.api.SecurityApi;
import org.killbill.commons.utils.annotation.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// By default, Kill Bill does NOT enforce authentication nor tenant information on /plugins/** requests
// (see KillBillShiroWebModule#configureShiroForRBAC and org.killbill.billing.osgi.http.OSGIServlet):
// it is up to each plugin to enforce its own access control. This filter requires HTTP Basic
// credentials to be validated against Kill Bill RBAC (the same realms used by /1.0/kb/**), without
// requiring tenant information (X-Killbill-ApiKey/ApiSecret), similar to NodesInfoResource.
class BasicAuthFilter implements Route.Filter {

    private static final Logger logger = LoggerFactory.getLogger(BasicAuthFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    private static final String WWW_AUTHENTICATE_VALUE = "Basic realm=\"".concat(Activator.PLUGIN_NAME).concat("\"");

    // Guards against a client (e.g. a buggy frontend that keeps reconnecting an SSE stream after
    // a failed auth, instead of giving up) hammering this endpoint with repeated failed attempts.
    private static final int MAX_FAILURES_PER_WINDOW = 10;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(1);

    private final SecurityApi securityApi;
    private final FailureRateLimiter rateLimiter;

    BasicAuthFilter(final SecurityApi securityApi) {
        this(securityApi, new FailureRateLimiter(MAX_FAILURES_PER_WINDOW, FAILURE_WINDOW));
    }

    @VisibleForTesting
    BasicAuthFilter(final SecurityApi securityApi, final FailureRateLimiter rateLimiter) {
        this.securityApi = securityApi;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void handle(final Request req, final Response rsp, final Route.Chain chain) throws Throwable {
        // CORS preflight requests never carry credentials (by the Fetch/CORS spec, browsers strip Authorization and
        // other request headers from the OPTIONS preflight itself), so enforcing auth here would always 401 the
        // preflight and block the real, cross-origin request before it's even sent.
        //
        // Respond 200 directly: there's no terminal route registered for OPTIONS  (the SSE route only handles GET),
        // so calling chain.next() would fall through to Jooby's own 405 Method Not Allowed, which also fails the
        // preflight (needs a 2xx response).
        //
        // The actual CORS headers (Access-Control-Allow-*) are added upstream by Kill Bill's ResponseCorsFilter.
        // We just need to not block the preflight with an auth check here.
        if ("OPTIONS".equalsIgnoreCase(req.method())) {
            rsp.status(200);
            rsp.end();
            return;
        }

        final String clientKey = req.ip();

        if (rateLimiter.isRateLimited(clientKey)) {
            this.handleTooManyRequests(rsp);
            return;
        }

        final Optional<BasicCredentials> credentials = BasicCredentials.fromRequest(req);

        // A missing/malformed Authorization header is not itself a rejection reason. Subject may already
        // be authenticated via an existing Shiro session (e.g. KAUI, which authenticates once via
        // /1.0/kb/security/permissions and reuses the session cookie on subsequent requests, including this one). So,
        // only attempt login when credentials are present, but always fall through to the isSubjectAuthenticated().
        credentials.ifPresent(this::login);

        if (!securityApi.isSubjectAuthenticated()) {
            rateLimiter.recordFailure(clientKey);
            this.handleUnAuthenticated(rsp);
            return;
        }

        rateLimiter.recordSuccess(clientKey);
        chain.next(req, rsp);
    }

    private void handleUnAuthenticated(final Response response) throws Throwable {
        response.header(WWW_AUTHENTICATE_HEADER, WWW_AUTHENTICATE_VALUE);
        response.status(401);
        response.send("Unauthorized");
    }

    private void handleTooManyRequests(final Response response) throws Throwable {
        response.header("Retry-After", String.valueOf(FAILURE_WINDOW.toSeconds()));
        response.status(429);
        response.send("Too Many Requests");
    }

    private void login(final BasicCredentials credentials) {
        try {
            securityApi.login(credentials.getUsername(), credentials.getPassword());
        } catch (final RuntimeException e) {
            // Invalid credentials: isSubjectAuthenticated() below will correctly report false and trigger the
            // 401 response. Also, we deliberately catch RuntimeException (not Shiro's AuthenticationException) since
            // this bundle has no compile dependency on shiro-core.
            logger.warn("SSE: invalid credentials");
        }
    }

    /**
     * Parses HTTP Basic credentials (username/password) out of a request's {@code Authorization} header. Returns
     * empty if the header is missing, not a {@code Basic} scheme, not valid base64, doesn't contain a colon-separated
     * {@code username:password} pair (the password itself may contain a colon), or if the username/password is empty.
     */
    @VisibleForTesting
    static class BasicCredentials {

        private final String username;
        private final String password;

        private BasicCredentials(final String username, final String password) {
            this.username = username;
            this.password = password;
        }

        String getUsername() {
            return username;
        }

        String getPassword() {
            return password;
        }

        static Optional<BasicCredentials> fromRequest(final Request req) {
            final Optional<String> authHeader = req.header(AUTHORIZATION_HEADER).toOptional();
            if (authHeader.isEmpty()) {
                return Optional.empty();
            }
            return decode(authHeader.get());
        }

        @VisibleForTesting
        static Optional<BasicCredentials> decode(final String authorizationHeaderValue) {
            // Auth-scheme tokens are matched case-insensitively (RFC 7235 section 2.1), so "basic ", "Basic " and
            // "BASIC " are all equally valid.
            if (!authorizationHeaderValue.regionMatches(true, 0, BASIC_PREFIX, 0, BASIC_PREFIX.length())) {
                return Optional.empty();
            }

            final String base64Credentials = authorizationHeaderValue.substring(BASIC_PREFIX.length()).trim();
            final String decoded;
            try {
                decoded = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            } catch (final IllegalArgumentException e) {
                return Optional.empty();
            }

            final int idx = decoded.indexOf(':');
            if (idx < 0) {
                return Optional.empty();
            }

            final String username = decoded.substring(0, idx);
            final String password = decoded.substring(idx + 1);
            if (username.isEmpty() || password.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new BasicCredentials(username, password));
        }
    }

    /**
     * Tracks failed-authentication counts per client key (see {@link Request#ip()}) within a fixed time window, to
     * blunt a client stuck in a reconnect loop against this endpoint (example: frontend bug that doesn't close an SSE
     * connection after a failed auth).
     *
     * Not a distributed rate limiter: state is in-process/per-node only, which is acceptable here since the goal is
     * containing one misbehaving client, not providing a hard security guarantee against a determined, IP-rotating
     * attacker.
     */
    @VisibleForTesting
    static class FailureRateLimiter {

        // Opportunistic cleanup trigger to bound memory growth from many distinct/expired keys, since entries are
        // otherwise only removed on access (lazy expiry). No scheduled background task is used here to avoid adding
        // lifecycle (start/stop) management to the plugin.
        private static final int CLEANUP_THRESHOLD = 10_000;

        // Minimum time between full-sweep cleanups once the threshold is hit. Without this, a burst of failures from
        // many distinct, still-unexpired keys (e.g. a high-cardinality/distributed attempt) would keep the map above
        // CLEANUP_THRESHOLD forever, turning every subsequent recordFailure() call into a wasted O(n) scan.
        private static final Duration MIN_CLEANUP_INTERVAL = Duration.ofSeconds(10);

        private final int maxFailures;
        private final Duration window;
        private final Clock clock; // Prefer date.time.Clock instead of killbill Clock
        private final ConcurrentHashMap<String, FailureWindow> failuresByKey = new ConcurrentHashMap<>();
        private final AtomicReference<Instant> lastCleanupAttempt = new AtomicReference<>(Instant.MIN);

        FailureRateLimiter(final int maxFailures, final Duration window) {
            this(maxFailures, window, Clock.systemUTC());
        }

        @VisibleForTesting
        FailureRateLimiter(final int maxFailures, final Duration window, final Clock clock) {
            this.maxFailures = maxFailures;
            this.window = window;
            this.clock = clock;
        }

        boolean isRateLimited(final String key) {
            final FailureWindow failureWindow = failuresByKey.get(key);
            return failureWindow != null && !isExpired(failureWindow) && failureWindow.count.get() >= maxFailures;
        }

        void recordFailure(final String key) {
            failuresByKey.compute(key, (k, existing) -> {
                if (existing == null || isExpired(existing)) {
                    return new FailureWindow(clock.instant());
                }
                existing.count.incrementAndGet();
                return existing;
            });

            if (failuresByKey.size() > CLEANUP_THRESHOLD) {
                final Instant now = clock.instant();
                final Instant previous = lastCleanupAttempt.get();
                // CAS, not just a volatile read-then-write: under a concurrent high-cardinality burst, multiple
                // threads could otherwise all observe the throttle interval as elapsed before any of them updates
                // the timestamp, and each would then redundantly run the full O(n) cleanup() scan -- defeating the
                // very throttle meant to bound that cost. Only the thread that wins the CAS proceeds.
                if (Duration.between(previous, now).compareTo(MIN_CLEANUP_INTERVAL) >= 0
                        && lastCleanupAttempt.compareAndSet(previous, now)) {
                    cleanup();
                }
            }
        }

        void recordSuccess(final String key) {
            failuresByKey.remove(key);
        }

        @VisibleForTesting
        int size() {
            return failuresByKey.size();
        }

        private boolean isExpired(final FailureWindow failureWindow) {
            return Duration.between(failureWindow.windowStart, clock.instant()).compareTo(window) >= 0;
        }

        private void cleanup() {
            failuresByKey.entrySet().removeIf(entry -> isExpired(entry.getValue()));
        }

        private static final class FailureWindow {

            private final Instant windowStart;
            private final AtomicInteger count = new AtomicInteger(1);

            private FailureWindow(final Instant windowStart) {
                this.windowStart = windowStart;
            }
        }
    }
}