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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.killbill.billing.ObjectType;
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.billing.util.api.RecordIdApi;
import org.killbill.billing.util.callcontext.TenantContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestLogsSseFilter {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();

    private RecordIdApi recordIdApi;
    private Response response;
    private Route.Chain chain;

    @BeforeMethod(groups = "fast")
    public void setUp() {
        recordIdApi = Mockito.mock(RecordIdApi.class);
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(tenantId), Mockito.eq(ObjectType.TENANT), Mockito.any())).thenReturn(5L);
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(accountId), Mockito.eq(ObjectType.ACCOUNT), Mockito.any())).thenReturn(42L);
        response = Mockito.mock(Response.class);
        Mockito.when(response.status(Mockito.anyInt())).thenReturn(response);
        chain = Mockito.mock(Route.Chain.class);
    }

    @Test(groups = "fast")
    public void testNoTenantNoParamsStreamsEverything() throws Throwable {
        final Request req = request(null, false, false);
        run(true, req);
        assertPassed(req, null);
        Mockito.verifyNoInteractions(recordIdApi);
    }

    @Test(groups = "fast")
    public void testValidTenantScopesByTenantRecordId() throws Throwable {
        final Request req = request(tenant(), true, true);
        run(true, req);
        final SseParamsCriteria filter = assertPassed(req, null);
        Assert.assertTrue(filter.match(entry("5", null, null)));
        Assert.assertFalse(filter.match(entry("6", null, null)));
    }

    @Test(groups = "fast")
    public void testApiHeadersWithoutTenantAttributeIs401() throws Throwable {
        run(true, request(null, true, true));
        assertRejected(401);
        run(true, request(null, true, false));
        run(true, request(null, false, true));
        Mockito.verify(response, Mockito.times(3)).status(401);
    }

    @Test(groups = "fast")
    public void testAccountIdWithoutTenantIs403() throws Throwable {
        run(true, request(null, false, false, "accountId", accountId.toString()));
        assertRejected(403);
    }

    @Test(groups = "fast")
    public void testAccountIdFilterUsesTenantContext() throws Throwable {
        final Request req = request(tenant(), true, true, "accountId", accountId.toString());
        run(true, req);
        final SseParamsCriteria filter = assertPassed(req, null);
        Assert.assertTrue(filter.match(entry("5", "42", null)));
        Assert.assertFalse(filter.match(entry("5", "43", null)));
        Assert.assertFalse(filter.match(entry("6", "42", null)));

        final ArgumentCaptor<TenantContext> ctx = ArgumentCaptor.forClass(TenantContext.class);
        Mockito.verify(recordIdApi).getRecordId(Mockito.eq(accountId), Mockito.eq(ObjectType.ACCOUNT), ctx.capture());
        Assert.assertEquals(ctx.getValue().getTenantId(), tenantId);
    }

    @Test(groups = "fast")
    public void testUnknownAccountStaysConnectedAndMatchesNothing() throws Throwable {
        final UUID unknown = UUID.randomUUID();
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(unknown), Mockito.eq(ObjectType.ACCOUNT), Mockito.any())).thenReturn(null);
        final Request req = request(tenant(), true, true, "accountId", unknown.toString());
        run(true, req);
        final SseParamsCriteria filter = assertPassed(req, null);
        Assert.assertFalse(filter.match(entry("5", "42", null)));
        Assert.assertFalse(filter.match(entry("5", null, null)));
    }

    @Test(groups = "fast")
    public void testMalformedAccountIdIs400() throws Throwable {
        run(true, request(tenant(), true, true, "accountId", "not-a-uuid"));
        assertRejected(400);
    }

    @Test(groups = "fast")
    public void testRepeatedParametersAre400() throws Throwable {
        run(true, request(tenant(), true, true, "accountId", accountId.toString(), accountId.toString()));
        run(true, request(tenant(), true, true, "userToken", "a", "b"));
        Mockito.verify(response, Mockito.times(2)).status(400);
        Mockito.verify(chain, Mockito.never()).next(Mockito.any(Request.class), Mockito.any(Response.class));
    }

    @Test(groups = "fast")
    public void testBlankParametersMeanNotProvided() throws Throwable {
        // Even without a tenant: a blank accountId must not trigger the 403
        for (final String blank : new String[]{"", " ", "  "}) {
            final Request req = request(null, false, false, "accountId", blank);
            run(true, req);
            assertPassed(req, null);
        }
        final Request req = request(null, false, false, "userToken", "");
        run(true, req);
        final SseParamsCriteria filter = assertPassed(req, null);
        Assert.assertFalse(filter.isActive());
    }

    @Test(groups = "fast")
    public void testUserTokenIsPlainStringAndNeedsNoTenant() throws Throwable {
        final Request req = request(null, false, false, "userToken", "not a uuid");
        run(true, req);
        final SseParamsCriteria filter = assertPassed(req, null);
        Assert.assertTrue(filter.match(entry(null, null, "not a uuid")));
        Assert.assertFalse(filter.match(entry(null, null, "other")));
    }

    @Test(groups = "fast")
    public void testSingleTenantModeSkipsTenantChecks() throws Throwable {
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(accountId), Mockito.eq(ObjectType.ACCOUNT), Mockito.any())).thenReturn(42L);
        final Request req = request(null, true, true, "accountId", accountId.toString());
        run(false, req);
        final SseParamsCriteria filter = assertPassed(req, null);
        Assert.assertTrue(filter.match(entry("0", "42", null)));

        final ArgumentCaptor<TenantContext> ctx = ArgumentCaptor.forClass(TenantContext.class);
        Mockito.verify(recordIdApi).getRecordId(Mockito.eq(accountId), Mockito.eq(ObjectType.ACCOUNT), ctx.capture());
        Assert.assertNull(ctx.getValue().getTenantId());
    }

    @Test(groups = "fast")
    public void testUnresolvableTenantIs500AndNeverStreams() throws Throwable {
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(tenantId), Mockito.eq(ObjectType.TENANT), Mockito.any())).thenReturn(null);
        run(true, request(tenant(), true, true));
        assertRejected(500);
        Mockito.verify(recordIdApi, Mockito.never()).getRecordId(Mockito.any(), Mockito.eq(ObjectType.ACCOUNT), Mockito.any());
    }

    // -- helpers --

    private void run(final boolean multiTenant, final Request req) throws Throwable {
        new LogsSseFilter(recordIdApi, multiTenant).handle(req, response, chain);
    }

    private void assertRejected(final int status) throws Throwable {
        Mockito.verify(response).status(status);
        Mockito.verify(chain, Mockito.never()).next(Mockito.any(Request.class), Mockito.any(Response.class));
    }

    private SseParamsCriteria assertPassed(final Request req, final Object unused) throws Throwable {
        Mockito.verify(response, Mockito.never()).status(Mockito.anyInt());
        Mockito.verify(chain).next(req, response);
        final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(req).set(Mockito.eq(SseParamsCriteria.REQUEST_KEY), captor.capture());
        return (SseParamsCriteria) captor.getValue();
    }

    private Tenant tenant() {
        final Tenant tenant = Mockito.mock(Tenant.class);
        Mockito.when(tenant.getId()).thenReturn(tenantId);
        return tenant;
    }

    private static LogEntryJson entry(final String tenant, final String account, final String token) {
        return new LogEntryJson(null, 3, "logger", "msg", token, tenant, account, null);
    }

    private static Request request(final Tenant tenant, final boolean apiKey, final boolean apiSecret, final String param, final String... values) {
        final Request req = request(tenant, apiKey, apiSecret);
        final Mutant mutant = Mockito.mock(Mutant.class);
        Mockito.when(mutant.isSet()).thenReturn(true);
        Mockito.when(mutant.toList(String.class)).thenReturn(List.of(values));
        Mockito.when(req.param(param)).thenReturn(mutant);
        return req;
    }

    private static Request request(final Tenant tenant, final boolean apiKey, final boolean apiSecret) {
        final Request req = Mockito.mock(Request.class);
        Mockito.when(req.method()).thenReturn("GET");
        Mockito.when(req.ifGet(LogsSseFilter.TENANT_ATTRIBUTE)).thenReturn(Optional.ofNullable(tenant));
        final Mutant keyHeader = header(apiKey);
        final Mutant secretHeader = header(apiSecret);
        Mockito.when(req.header(LogsSseFilter.API_KEY_HEADER)).thenReturn(keyHeader);
        Mockito.when(req.header(LogsSseFilter.API_SECRET_HEADER)).thenReturn(secretHeader);
        for (final String name : new String[]{"accountId", "userToken"}) {
            final Mutant absent = Mockito.mock(Mutant.class);
            Mockito.when(absent.isSet()).thenReturn(false);
            Mockito.when(req.param(name)).thenReturn(absent);
        }
        return req;
    }

    private static Mutant header(final boolean present) {
        final Mutant mutant = Mockito.mock(Mutant.class);
        Mockito.when(mutant.isSet()).thenReturn(present);
        return mutant;
    }
}
