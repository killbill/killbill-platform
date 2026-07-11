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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooby.Mutant;
import org.jooby.Request;
import org.killbill.billing.ObjectType;
import org.killbill.billing.util.api.RecordIdApi;
import org.killbill.commons.utils.collect.Iterables;
import org.mockito.Mockito;
import org.osgi.service.log.LogService;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestLogEntriesFilter {

    // -- match() tests --

    @Test(groups = "fast")
    public void testMatchNoFilters() {
        final LogEntriesFilter filter = buildFilter(null, null);
        Assert.assertTrue(filter.match(makeEntry("msg", "token-1", "42")));
    }

    @Test(groups = "fast")
    public void testMatchByUserTokenOnly() {
        final LogEntriesFilter filter = buildFilter(null, "token-1");
        Assert.assertTrue(filter.match(makeEntry("msg", "token-1", "42")));
        Assert.assertFalse(filter.match(makeEntry("msg", "other", "42")));
    }

    @Test(groups = "fast")
    public void testMatchByAccountRecordIdOnly() {
        final UUID accountId = UUID.randomUUID();
        final LogEntriesFilter filter = buildFilterWithAccount(accountId, 42L, null);
        Assert.assertTrue(filter.match(makeEntry("msg", "token-1", "42")));
        Assert.assertFalse(filter.match(makeEntry("msg", "token-1", "99")));
    }

    @Test(groups = "fast")
    public void testMatchBothFiltersOr() {
        final UUID accountId = UUID.randomUUID();
        final LogEntriesFilter filter = buildFilterWithAccount(accountId, 42L, "token-1");
        // Both match
        Assert.assertTrue(filter.match(makeEntry("msg", "token-1", "42")));
        // accountRecordId matches only
        Assert.assertTrue(filter.match(makeEntry("msg", "wrong", "42")));
        // userToken matches only
        Assert.assertTrue(filter.match(makeEntry("msg", "token-1", "99")));
        // Neither matches
        Assert.assertFalse(filter.match(makeEntry("msg", "wrong", "99")));
    }

    @Test(groups = "fast")
    public void testMatchEntryWithNullFields() {
        final LogEntriesFilter noFilter = buildFilter(null, null);
        Assert.assertTrue(noFilter.match(makeEntry("msg", null, null)));

        final LogEntriesFilter tokenFilter = buildFilter(null, "any");
        Assert.assertFalse(tokenFilter.match(makeEntry("msg", null, null)));

        final UUID accountId = UUID.randomUUID();
        final LogEntriesFilter accountFilter = buildFilterWithAccount(accountId, 42L, null);
        Assert.assertFalse(accountFilter.match(makeEntry("msg", null, null)));
    }

    // -- getNonBlankParam() tests --

    @Test(groups = "fast")
    public void testGetNonBlankParamPresent() {
        final Request request = mockRequestWithParam("userToken", "abc");
        Assert.assertEquals(LogEntriesFilter.getNonBlankParam(request, "userToken"), Optional.of("abc"));
    }

    @Test(groups = "fast")
    public void testGetNonBlankParamEmpty() {
        final Request request = mockRequestWithParam("userToken", "");
        Assert.assertEquals(LogEntriesFilter.getNonBlankParam(request, "userToken"), Optional.empty());
    }

    @Test(groups = "fast")
    public void testGetNonBlankParamBlank() {
        final Request request = mockRequestWithParam("userToken", "   ");
        Assert.assertEquals(LogEntriesFilter.getNonBlankParam(request, "userToken"), Optional.empty());
    }

    @Test(groups = "fast")
    public void testGetNonBlankParamMissing() {
        final Request request = mockRequestWithParam("userToken", null);
        Assert.assertEquals(LogEntriesFilter.getNonBlankParam(request, "userToken"), Optional.empty());
    }

    // -- resolveAccountRecordId() tests --

    @Test(groups = "fast")
    public void testResolveAccountRecordIdValid() {
        final UUID accountId = UUID.randomUUID();
        final Request request = mockRequestWithParam("accountId", accountId.toString());
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(accountId), Mockito.eq(ObjectType.ACCOUNT), Mockito.any()))
               .thenReturn(42L);

        final LogEntriesFilter.AccountRecordIdValue result = LogEntriesFilter.resolveAccountRecordId(request, recordIdApi);
        Assert.assertTrue(result.isRequested());
        Assert.assertTrue(result.matches("42"));
        Assert.assertFalse(result.matches("99"));
    }

    @Test(groups = "fast")
    public void testResolveAccountRecordIdNotFound() {
        final UUID accountId = UUID.randomUUID();
        final Request request = mockRequestWithParam("accountId", accountId.toString());
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(accountId), Mockito.eq(ObjectType.ACCOUNT), Mockito.any()))
               .thenReturn(null);

        final LogEntriesFilter.AccountRecordIdValue result = LogEntriesFilter.resolveAccountRecordId(request, recordIdApi);
        Assert.assertTrue(result.isRequested(), "Filter should be active for provided but unresolvable accountId");
        Assert.assertFalse(result.matches("42"), "Unresolvable accountId should never match");
    }

    @Test(groups = "fast")
    public void testResolveAccountRecordIdZero() {
        final UUID accountId = UUID.randomUUID();
        final Request request = mockRequestWithParam("accountId", accountId.toString());
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(accountId), Mockito.eq(ObjectType.ACCOUNT), Mockito.any()))
               .thenReturn(0L);

        final LogEntriesFilter.AccountRecordIdValue result = LogEntriesFilter.resolveAccountRecordId(request, recordIdApi);
        Assert.assertTrue(result.isRequested());
        Assert.assertFalse(result.matches("0"));
    }

    @Test(groups = "fast")
    public void testResolveAccountRecordIdInvalidUuid() {
        final Request request = mockRequestWithParam("accountId", "not-a-uuid");
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);

        final LogEntriesFilter.AccountRecordIdValue result = LogEntriesFilter.resolveAccountRecordId(request, recordIdApi);
        Assert.assertTrue(result.isRequested(), "Filter should be active for invalid UUID");
        Assert.assertFalse(result.matches("42"));
        Mockito.verifyNoInteractions(recordIdApi);
    }

    @Test(groups = "fast")
    public void testResolveAccountRecordIdBlankParam() {
        final Request request = mockRequestWithParam("accountId", "");
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);

        final LogEntriesFilter.AccountRecordIdValue result = LogEntriesFilter.resolveAccountRecordId(request, recordIdApi);
        Assert.assertFalse(result.isRequested(), "Blank param means not requested");
        Mockito.verifyNoInteractions(recordIdApi);
    }

    // -- apply() tests --

    @Test(groups = "fast")
    public void testApplyNoFilter() {
        final Request request = mockRequestWithParams(null, null);
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);
        final LogEntriesFilter filter = new LogEntriesFilter(request, recordIdApi);

        final List<LogEntryJson> entries = List.of(
                makeEntry("msg1", "token-a", "10"),
                makeEntry("msg2", null, null));

        final Iterable<LogEntryJson> result = filter.apply(entries);
        Assert.assertSame(result, entries, "No filter should return same iterable");
        Assert.assertFalse(filter.hasNoResult());
    }

    @Test(groups = "fast")
    public void testApplyFilterByUserToken() {
        final Request request = mockRequestWithParams(null, "my-token");
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);
        final LogEntriesFilter filter = new LogEntriesFilter(request, recordIdApi);

        final List<LogEntryJson> entries = List.of(
                makeEntry("match", "my-token", null),
                makeEntry("skip", "other", null),
                makeEntry("skip2", null, null));

        final List<LogEntryJson> result = Iterables.toUnmodifiableList(filter.apply(entries));
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getMessage(), "match");
        Assert.assertFalse(filter.hasNoResult());
    }

    @Test(groups = "fast")
    public void testApplyFilterNothingMatches() {
        final Request request = mockRequestWithParams(null, "non-existent");
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);
        final LogEntriesFilter filter = new LogEntriesFilter(request, recordIdApi);

        final List<LogEntryJson> entries = List.of(
                makeEntry("msg1", "other", null),
                makeEntry("msg2", null, null));

        final List<LogEntryJson> result = Iterables.toUnmodifiableList(filter.apply(entries));
        Assert.assertTrue(result.isEmpty());
        Assert.assertTrue(filter.hasNoResult());
    }

    @Test(groups = "fast")
    public void testApplyHasNoResultResetsPerCycle() {
        final Request request = mockRequestWithParams(null, "my-token");
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);
        final LogEntriesFilter filter = new LogEntriesFilter(request, recordIdApi);

        // Cycle 1: match
        filter.apply(List.of(makeEntry("match", "my-token", null)));
        Assert.assertFalse(filter.hasNoResult());

        // Cycle 2: no match — hasNoResult must reset to true
        filter.apply(List.of(makeEntry("skip", "other", null)));
        Assert.assertTrue(filter.hasNoResult());

        // Cycle 3: match again
        filter.apply(List.of(makeEntry("match2", "my-token", null)));
        Assert.assertFalse(filter.hasNoResult());
    }

    @Test(groups = "fast")
    public void testApplyOrLogicBothFilters() {
        final UUID accountId = UUID.randomUUID();
        final Request request = mockRequestWithParams(accountId.toString(), "my-token");
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(accountId), Mockito.eq(ObjectType.ACCOUNT), Mockito.any()))
               .thenReturn(42L);

        final LogEntriesFilter filter = new LogEntriesFilter(request, recordIdApi);

        final List<LogEntryJson> entries = List.of(
                makeEntry("both match", "my-token", "42"),
                makeEntry("token only", "my-token", "99"),
                makeEntry("account only", "other", "42"),
                makeEntry("neither", "other", "99"));

        final List<LogEntryJson> result = Iterables.toUnmodifiableList(filter.apply(entries));
        Assert.assertEquals(result.size(), 3);
        Assert.assertEquals(result.get(0).getMessage(), "both match");
        Assert.assertEquals(result.get(1).getMessage(), "token only");
        Assert.assertEquals(result.get(2).getMessage(), "account only");
        Assert.assertFalse(filter.hasNoResult());
    }

    @Test(groups = "fast")
    public void testApplyUnresolvableAccountIdMatchesNothing() {
        final UUID unknownAccountId = UUID.randomUUID();
        final Request request = mockRequestWithParams(unknownAccountId.toString(), null);
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(unknownAccountId), Mockito.eq(ObjectType.ACCOUNT), Mockito.any()))
               .thenReturn(null);

        final LogEntriesFilter filter = new LogEntriesFilter(request, recordIdApi);

        final List<LogEntryJson> entries = List.of(
                makeEntry("msg1", "token", "42"),
                makeEntry("msg2", null, null));

        // Unresolvable accountId → sentinel keeps filter active → nothing matches
        final List<LogEntryJson> result = Iterables.toUnmodifiableList(filter.apply(entries));
        Assert.assertTrue(result.isEmpty());
        Assert.assertTrue(filter.hasNoResult());
    }

    // -- Helpers --

    private static LogEntryJson makeEntry(final String message, final String userToken, final String accountRecordId) {
        return new LogEntryJson(null, LogService.LOG_INFO, "test.Logger", message, userToken, null, accountRecordId, null);
    }

    private static LogEntriesFilter buildFilter(final String accountId, final String userToken) {
        final Request request = mockRequestWithParams(accountId, userToken);
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);
        return new LogEntriesFilter(request, recordIdApi);
    }

    private static LogEntriesFilter buildFilterWithAccount(final UUID accountId, final Long recordId, final String userToken) {
        final Request request = mockRequestWithParams(accountId.toString(), userToken);
        final RecordIdApi recordIdApi = Mockito.mock(RecordIdApi.class);
        Mockito.when(recordIdApi.getRecordId(Mockito.eq(accountId), Mockito.eq(ObjectType.ACCOUNT), Mockito.any()))
               .thenReturn(recordId);
        return new LogEntriesFilter(request, recordIdApi);
    }

    private static Request mockRequestWithParam(final String name, final String value) {
        final Request request = Mockito.mock(Request.class);
        final Mutant mutant = Mockito.mock(Mutant.class);
        Mockito.when(mutant.toOptional()).thenReturn(value == null ? Optional.empty() : Optional.of(value));
        Mockito.when(request.param(name)).thenReturn(mutant);
        return request;
    }

    private static Request mockRequestWithParams(final String accountId, final String userToken) {
        final Request request = Mockito.mock(Request.class);

        final Mutant accountMutant = Mockito.mock(Mutant.class);
        Mockito.when(accountMutant.toOptional()).thenReturn(accountId == null ? Optional.empty() : Optional.of(accountId));
        Mockito.when(request.param("accountId")).thenReturn(accountMutant);

        final Mutant tokenMutant = Mockito.mock(Mutant.class);
        Mockito.when(tokenMutant.toOptional()).thenReturn(userToken == null ? Optional.empty() : Optional.of(userToken));
        Mockito.when(request.param("userToken")).thenReturn(tokenMutant);

        return request;
    }
}
