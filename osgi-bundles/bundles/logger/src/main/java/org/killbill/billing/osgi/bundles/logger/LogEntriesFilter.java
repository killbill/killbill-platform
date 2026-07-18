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

import org.jooby.Request;
import org.killbill.billing.ObjectType;
import org.killbill.billing.plugin.api.PluginTenantContext;
import org.killbill.billing.util.api.RecordIdApi;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.commons.utils.annotation.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;

// http://localhost:8080/killbill-osgi-logger/ -> no filter applied
//
// http://localhost:8080/killbill-osgi-logger/?accountId -> no filter applied
// http://localhost:8080/killbill-osgi-logger/?accountId= -> no filter applied
// http://localhost:8080/killbill-osgi-logger/?accountId=somename -> filter by accountId. Return if matched. If nothing found, then do not return anything (heartbeat)
//
// http://localhost:8080/killbill-osgi-logger/?userToken -> no filter
// http://localhost:8080/killbill-osgi-logger/?userToken= -> no filter
// http://localhost:8080/killbill-osgi-logger/?userToken=someToken -> filter by userToken. Return if matched. If nothing found, then do not return anything (heartbeat)
//
// http://localhost:8080/killbill-osgi-logger/?accountId&userToken -> no filter applied
// http://localhost:8080/killbill-osgi-logger/?accountId=&userToken -> no filter applied
// http://localhost:8080/killbill-osgi-logger/?accountId&userToken= -> no filter applied
// http://localhost:8080/killbill-osgi-logger/?accountId=&userToken= -> no filter applied
//
// http://localhost:8080/killbill-osgi-logger/?accountId=somename&userToken -> filter by accountId only. Return if matched. If nothing found, then do not return anything (heartbeat)
// http://localhost:8080/killbill-osgi-logger/?accountId=somename&userToken= -> filter by accountId only. Return if matched. If nothing found, then do not return anything (heartbeat)
//
// http://localhost:8080/killbill-osgi-logger/?accountId&userToken=someToken -> filter by userToken only. Return if matched. If nothing found, then do not return anything (heartbeat)
// http://localhost:8080/killbill-osgi-logger/?accountId=&userToken=someToken -> filter by userToken only. Return if matched. If nothing found, then do not return anything (heartbeat)
//
// http://localhost:8080/killbill-osgi-logger/?accountId=somename&userToken=someToken -> filter by accountId OR userToken:
// - if by accountId found, return it.
// - if by accountId not found but by userToken found, return it.
// - If nothing found, then do not return anything (heartbeat)
class LogEntriesFilter {

    private static final Logger logger = LoggerFactory.getLogger(LogEntriesFilter.class);

    private final AccountRecordIdValue accountRecordId;
    private final Optional<String> userToken;
    private boolean noResult;

    LogEntriesFilter(final Request request, final RecordIdApi recordIdApi) {
        // resolveAccountRecordId() very likely call to db. Doing this at apply() time will cause it called every
        // SSE calls.
        this.accountRecordId = resolveAccountRecordId(request, recordIdApi);
        this.userToken = getNonBlankParam(request, "userToken");
        this.noResult = true;
    }

    public boolean hasNoResult() {
        return noResult;
    }

    /**
     * Filters entries against the resolved criteria.
     * Returns matching entries; if no filter is active, returns all.
     */
    public Iterable<LogEntryJson> apply(final Iterable<LogEntryJson> entries) {
        // Reset per drain cycle so heartbeat logic in the handler works correctly
        noResult = true;

        if (!accountRecordId.isRequested() && userToken.isEmpty()) {
            noResult = false;
            return entries;
        }

        final List<LogEntryJson> result = new ArrayList<>();
        for (final LogEntryJson entry : entries) {
            if (match(entry)) {
                result.add(entry);
                noResult = false;
            }
        }
        return result;
    }

    /**
     * OR logic: entry passes if it matches ANY active filter.
     * If no filter is active, all entries pass.
     */
    @VisibleForTesting
    boolean match(final LogEntryJson entry) {
        if (!accountRecordId.isRequested() && userToken.isEmpty()) {
            return true;
        }
        if (accountRecordId.matches(entry.getAccountRecordId())) {
            return true;
        }
        return userToken.isPresent() && userToken.get().equals(entry.getUserToken());
    }

    /**
     * Resolves the {@code accountId} query param to an internal record ID.
     * Returns empty if param is missing or blank.
     * Returns a sentinel value if param is provided but unresolvable (invalid UUID or account not found),
     * so the filter stays active and matches nothing.
     */
    @VisibleForTesting
    static AccountRecordIdValue resolveAccountRecordId(final Request request, final RecordIdApi recordIdApi) {
        final Optional<String> param = getNonBlankParam(request, "accountId");
        logger.debug("param.isPresent(): {}, value: {} ", param.isPresent(), param.orElse("<null>"));
        if (param.isEmpty()) {
            return AccountRecordIdValue.empty();
        }

        try {
            final UUID accountId = UUID.fromString(param.get());
            logger.debug("accountId UUID constructed");
            final TenantContext context = new PluginTenantContext(accountId, null);
            final Long recordId = recordIdApi.getRecordId(accountId, ObjectType.ACCOUNT, context);
            logger.debug("recordId: {}", recordId);
            if (recordId != null && recordId > 0L) {
                return AccountRecordIdValue.exist(recordId.toString());
            }
        } catch (final IllegalArgumentException ignored) {
            // Invalid UUID format
            logger.info("receive invalid UUID as request parameter");
        }

        // accountId was provided but accountRecordId unresolvable. keep filter active with a value that never matches
        return AccountRecordIdValue.notMatch();
    }

    /**
     * Returns the param value only if it is present and non-blank.
     * Treats missing, empty, or whitespace-only values as "not provided".
     */
    @VisibleForTesting
    static Optional<String> getNonBlankParam(final Request request, final String name) {
        final Optional<String> value = request.param(name).toOptional();
        if (value.isPresent() && !value.get().isBlank()) {
            return value;
        }
        return Optional.empty();
    }

    static class AccountRecordIdValue {

        enum State {
            EMPTY, // SSE request parameter not set
            NOT_MATCH, // accountId not found
            EXIST
        }

        private final State state;
        private final String value;

        AccountRecordIdValue(final State state, final String value) {
            this.state = state;
            this.value = value;
        }

        static AccountRecordIdValue empty() {
            return new AccountRecordIdValue(State.EMPTY, null);
        }

        static AccountRecordIdValue notMatch() {
            return new AccountRecordIdValue(State.NOT_MATCH, null);
        }

        static AccountRecordIdValue exist(@Nonnull final String value) {
            return new AccountRecordIdValue(State.EXIST, value);
        }

        /** True when the client provided a non-blank accountId param. */
        boolean isRequested() {
            return state != State.EMPTY;
        }

        /** True only when the accountId was resolved and its record ID equals the given value. */
        boolean matches(final String entryAccountRecordId) {
            return state == State.EXIST && value.equals(entryAccountRecordId);
        }
    }
}
