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

import java.util.Base64;
import java.util.Optional;

import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.killbill.billing.security.api.SecurityApi;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBasicAuthFilter {

    // -- handle() tests --

    @Test(groups = "fast")
    public void testHandleNoAuthorizationHeaderRejected() throws Throwable {
        final SecurityApi securityApi = Mockito.mock(SecurityApi.class);
        Mockito.when(securityApi.isSubjectAuthenticated()).thenReturn(false);

        final Request request = mockRequestWithAuthorizationHeader(null);
        final Response response = Mockito.mock(Response.class);
        Mockito.when(response.header(Mockito.anyString(), Mockito.any(Object.class))).thenReturn(response);
        Mockito.when(response.status(Mockito.anyInt())).thenReturn(response);
        final Route.Chain chain = Mockito.mock(Route.Chain.class);

        new BasicAuthFilter(securityApi).handle(request, response, chain);

        Mockito.verify(securityApi, Mockito.never()).login(Mockito.any(), Mockito.any());
        Mockito.verify(response).status(401);
        Mockito.verify(response).header("WWW-Authenticate", "Basic realm=\"killbill-osgi-logger\"");
        Mockito.verify(chain, Mockito.never()).next(Mockito.any(Request.class), Mockito.any(Response.class));
    }

    @Test(groups = "fast")
    public void testHandleNoAuthorizationHeaderButAlreadyAuthenticatedAllowed() throws Throwable {
        // No Authorization header on this request, but the Subject is already authenticated,
        // e.g. via an existing Shiro session cookie (Kaui authenticates once via
        // /1.0/kb/security/permissions and reuses the session on subsequent requests). This must
        // not be rejected: login() should not even be attempted when there's no header, and the
        // pre-existing authenticated state alone should be enough to let the request through.
        final SecurityApi securityApi = Mockito.mock(SecurityApi.class);
        Mockito.when(securityApi.isSubjectAuthenticated()).thenReturn(true);

        final Request request = mockRequestWithAuthorizationHeader(null);
        final Response response = Mockito.mock(Response.class);
        final Route.Chain chain = Mockito.mock(Route.Chain.class);

        new BasicAuthFilter(securityApi).handle(request, response, chain);

        Mockito.verify(securityApi, Mockito.never()).login(Mockito.any(), Mockito.any());
        Mockito.verify(response, Mockito.never()).status(Mockito.anyInt());
        Mockito.verify(chain).next(request, response);
    }

    @Test(groups = "fast")
    public void testHandleValidCredentialsAllowed() throws Throwable {
        final SecurityApi securityApi = Mockito.mock(SecurityApi.class);
        Mockito.when(securityApi.isSubjectAuthenticated()).thenReturn(true);

        final Request request = mockRequestWithAuthorizationHeader(basicHeader("admin", "password"));
        final Response response = Mockito.mock(Response.class);
        final Route.Chain chain = Mockito.mock(Route.Chain.class);

        new BasicAuthFilter(securityApi).handle(request, response, chain);

        Mockito.verify(securityApi).login("admin", "password");
        Mockito.verify(chain).next(request, response);
        Mockito.verify(response, Mockito.never()).status(Mockito.anyInt());
    }

    @Test(groups = "fast")
    public void testHandleInvalidCredentialsRejected() throws Throwable {
        final SecurityApi securityApi = Mockito.mock(SecurityApi.class);
        Mockito.doThrow(new RuntimeException("bad credentials")).when(securityApi).login(Mockito.any(), Mockito.any());
        Mockito.when(securityApi.isSubjectAuthenticated()).thenReturn(false);

        final Request request = mockRequestWithAuthorizationHeader(basicHeader("admin", "wrong"));
        final Response response = Mockito.mock(Response.class);
        Mockito.when(response.header(Mockito.anyString(), Mockito.any(Object.class))).thenReturn(response);
        Mockito.when(response.status(Mockito.anyInt())).thenReturn(response);
        final Route.Chain chain = Mockito.mock(Route.Chain.class);

        new BasicAuthFilter(securityApi).handle(request, response, chain);

        Mockito.verify(securityApi).login("admin", "wrong");
        Mockito.verify(response).status(401);
        Mockito.verify(chain, Mockito.never()).next(Mockito.any(Request.class), Mockito.any(Response.class));
    }

    @Test(groups = "fast")
    public void testHandleMalformedAuthorizationHeaderStillChecksAuthentication() throws Throwable {
        final SecurityApi securityApi = Mockito.mock(SecurityApi.class);
        Mockito.when(securityApi.isSubjectAuthenticated()).thenReturn(false);

        final Request request = mockRequestWithAuthorizationHeader("Bearer sometoken");
        final Response response = Mockito.mock(Response.class);
        Mockito.when(response.header(Mockito.anyString(), Mockito.any(Object.class))).thenReturn(response);
        Mockito.when(response.status(Mockito.anyInt())).thenReturn(response);
        final Route.Chain chain = Mockito.mock(Route.Chain.class);

        new BasicAuthFilter(securityApi).handle(request, response, chain);

        Mockito.verify(securityApi, Mockito.never()).login(Mockito.any(), Mockito.any());
        Mockito.verify(response).status(401);
    }

    // -- CORS preflight --

    @Test(groups = "fast")
    public void testHandleOptionsRequestBypassesAuth() throws Throwable {
        // A CORS preflight (OPTIONS) never carries the Authorization header (stripped by the
        // browser), so it must be let through unconditionally, or legitimate cross-origin clients
        // sending Basic Auth would never get past the preflight in the first place. It must also
        // get a 2xx response directly (not chain.next(), which would fall through to Jooby's own
        // 405, since no terminal route is registered for OPTIONS).
        final SecurityApi securityApi = Mockito.mock(SecurityApi.class);

        final Request request = mockRequestWithAuthorizationHeader(null);
        Mockito.when(request.method()).thenReturn("OPTIONS");
        final Response response = Mockito.mock(Response.class);
        final Route.Chain chain = Mockito.mock(Route.Chain.class);

        new BasicAuthFilter(securityApi).handle(request, response, chain);

        Mockito.verify(securityApi, Mockito.never()).login(Mockito.any(), Mockito.any());
        Mockito.verify(securityApi, Mockito.never()).isSubjectAuthenticated();
        Mockito.verify(response).status(200);
        Mockito.verify(response).end();
        Mockito.verify(chain, Mockito.never()).next(Mockito.any(Request.class), Mockito.any(Response.class));
    }

    // -- rate limiting integration tests --

    @Test(groups = "fast")
    public void testHandleRateLimitedReturns429BeforeLogin() throws Throwable {
        final SecurityApi securityApi = Mockito.mock(SecurityApi.class);
        final BasicAuthFilter.FailureRateLimiter rateLimiter = Mockito.mock(BasicAuthFilter.FailureRateLimiter.class);
        Mockito.when(rateLimiter.isRateLimited("1.2.3.4")).thenReturn(true);

        final Request request = mockRequestWithAuthorizationHeader(basicHeader("admin", "password"), "1.2.3.4");
        final Response response = Mockito.mock(Response.class);
        Mockito.when(response.header(Mockito.anyString(), Mockito.any(Object.class))).thenReturn(response);
        Mockito.when(response.status(Mockito.anyInt())).thenReturn(response);
        final Route.Chain chain = Mockito.mock(Route.Chain.class);

        new BasicAuthFilter(securityApi, rateLimiter).handle(request, response, chain);

        Mockito.verify(securityApi, Mockito.never()).login(Mockito.any(), Mockito.any());
        Mockito.verify(response).status(429);
        Mockito.verify(response).header("Retry-After", "60");
        Mockito.verify(chain, Mockito.never()).next(Mockito.any(Request.class), Mockito.any(Response.class));
    }

    @Test(groups = "fast")
    public void testHandleFailureRecordedOnRejection() throws Throwable {
        final SecurityApi securityApi = Mockito.mock(SecurityApi.class);
        Mockito.when(securityApi.isSubjectAuthenticated()).thenReturn(false);
        final BasicAuthFilter.FailureRateLimiter rateLimiter = Mockito.mock(BasicAuthFilter.FailureRateLimiter.class);

        final Request request = mockRequestWithAuthorizationHeader(basicHeader("admin", "wrong"), "1.2.3.4");
        final Response response = Mockito.mock(Response.class);
        Mockito.when(response.header(Mockito.anyString(), Mockito.any(Object.class))).thenReturn(response);
        Mockito.when(response.status(Mockito.anyInt())).thenReturn(response);
        final Route.Chain chain = Mockito.mock(Route.Chain.class);

        new BasicAuthFilter(securityApi, rateLimiter).handle(request, response, chain);

        Mockito.verify(rateLimiter).recordFailure("1.2.3.4");
        Mockito.verify(rateLimiter, Mockito.never()).recordSuccess(Mockito.any());
    }

    @Test(groups = "fast")
    public void testHandleSuccessRecordedAndChainContinues() throws Throwable {
        final SecurityApi securityApi = Mockito.mock(SecurityApi.class);
        Mockito.when(securityApi.isSubjectAuthenticated()).thenReturn(true);
        final BasicAuthFilter.FailureRateLimiter rateLimiter = Mockito.mock(BasicAuthFilter.FailureRateLimiter.class);

        final Request request = mockRequestWithAuthorizationHeader(basicHeader("admin", "password"), "1.2.3.4");
        final Response response = Mockito.mock(Response.class);
        final Route.Chain chain = Mockito.mock(Route.Chain.class);

        new BasicAuthFilter(securityApi, rateLimiter).handle(request, response, chain);

        Mockito.verify(rateLimiter).recordSuccess("1.2.3.4");
        Mockito.verify(rateLimiter, Mockito.never()).recordFailure(Mockito.any());
        Mockito.verify(chain).next(request, response);
    }

    // -- helpers --

    private static String basicHeader(final String username, final String password) {
        final String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        return "Basic " + encoded;
    }

    private static Request mockRequestWithAuthorizationHeader(final String value) {
        return mockRequestWithAuthorizationHeader(value, "127.0.0.1");
    }

    private static Request mockRequestWithAuthorizationHeader(final String value, final String ip) {
        final Request request = Mockito.mock(Request.class);
        final Mutant mutant = Mockito.mock(Mutant.class);
        Mockito.when(mutant.toOptional()).thenReturn(value == null ? Optional.empty() : Optional.of(value));
        Mockito.when(request.header("Authorization")).thenReturn(mutant);
        Mockito.when(request.ip()).thenReturn(ip);
        return request;
    }
}
