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

import javax.annotation.Nullable;

// Immutable, resolved once per SSE connection by LogsSseFilter. All active criteria are combined with AND, and an
// entry lacking an attribute that is being filtered on is excluded.
class SseParamsCriteria {

    static final String REQUEST_KEY = SseParamsCriteria.class.getName();

    static final SseParamsCriteria NONE = new SseParamsCriteria(null, false, null, null);

    private final String tenantRecordId;
    private final boolean accountRequested;
    private final String accountRecordId;
    private final String userToken;

    // tenantRecordId: null means no tenant scoping.
    // accountRequested: true when accountId was provided; accountRecordId null then means "no such account" (matches nothing).
    // userToken: null means no token filtering.
    SseParamsCriteria(@Nullable final String tenantRecordId,
                     final boolean accountRequested,
                     @Nullable final String accountRecordId,
                     @Nullable final String userToken) {
        this.tenantRecordId = tenantRecordId;
        this.accountRequested = accountRequested;
        this.accountRecordId = accountRecordId;
        this.userToken = userToken;
    }

    boolean isActive() {
        return tenantRecordId != null || accountRequested || userToken != null;
    }

    boolean match(final LogEntryJson entry) {
        if (tenantRecordId != null && !tenantRecordId.equals(entry.getTenantRecordId())) {
            return false;
        }
        if (accountRequested && (accountRecordId == null || !accountRecordId.equals(entry.getAccountRecordId()))) {
            return false;
        }
        return userToken == null || userToken.equals(entry.getUserToken());
    }

    Iterable<LogEntryJson> apply(final Iterable<LogEntryJson> entries) {
        if (!isActive()) {
            return entries;
        }
        final List<LogEntryJson> result = new ArrayList<>();
        for (final LogEntryJson entry : entries) {
            if (match(entry)) {
                result.add(entry);
            }
        }
        return result;
    }
}
