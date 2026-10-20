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
import org.killbill.billing.plugin.api.PluginTenantContext;
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.billing.util.api.RecordIdApi;
import org.killbill.billing.util.callcontext.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Validates the filtering parameters (accountId, userToken) and tenant headers, and resolves them once per connection
// into a SseParamsCriteria stored on the request. This must be a filter (not done in the SSE handler): by the time the
// handler runs, the 200 status and text/event-stream content type have already been sent, so errors can't be returned.
//
// Evaluation order (first failure answers): 401 (tenant headers not validated), 403 (accountId without tenant), 400 (syntax).
class LogsSseFilter implements Route.Filter {

    private static final Logger logger = LoggerFactory.getLogger(LogsSseFilter.class);

    static final String TENANT_ATTRIBUTE = "killbill_tenant"; // Set by Kill Bill's TenantFilter after validating the API key/secret
    static final String API_KEY_HEADER = "X-Killbill-ApiKey";
    static final String API_SECRET_HEADER = "X-Killbill-ApiSecret";
    static final String ACCOUNT_ID_PARAM = "accountId";
    static final String USER_TOKEN_PARAM = "userToken";

    private final RecordIdApi recordIdApi;
    private final boolean multiTenant;

    LogsSseFilter(final RecordIdApi recordIdApi, final boolean multiTenant) {
        this.recordIdApi = recordIdApi;
        this.multiTenant = multiTenant;
    }

    @Override
    public void handle(final Request req, final Response rsp, final Route.Chain chain) throws Throwable {
        // CORS preflight is answered by BasicAuthFilter and never reaches here, but be safe if the order changes
        if ("OPTIONS".equalsIgnoreCase(req.method())) {
            chain.next(req, rsp);
            return;
        }

        final Tenant tenant = req.<Tenant>ifGet(TENANT_ATTRIBUTE).orElse(null);

        // On plugin paths TenantFilter lets a request with a wrong or half-supplied key/secret through, without tenant
        if (multiTenant && tenant == null && (req.header(API_KEY_HEADER).isSet() || req.header(API_SECRET_HEADER).isSet())) {
            reject(rsp, 401, "Invalid " + API_KEY_HEADER + " / " + API_SECRET_HEADER);
            return;
        }

        final List<String> accountIds = values(req, ACCOUNT_ID_PARAM);
        final List<String> userTokens = values(req, USER_TOKEN_PARAM);
        final String accountId = nonBlank(accountIds);
        final String userToken = nonBlank(userTokens);

        // accountId lookups are tenant-scoped
        if (multiTenant && tenant == null && accountId != null) {
            reject(rsp, 403, ACCOUNT_ID_PARAM + " requires " + API_KEY_HEADER + " / " + API_SECRET_HEADER);
            return;
        }

        if (accountIds.size() > 1 || userTokens.size() > 1) {
            reject(rsp, 400, "Parameters " + ACCOUNT_ID_PARAM + " and " + USER_TOKEN_PARAM + " can be specified only once");
            return;
        }

        final UUID accountUuid;
        if (accountId == null) {
            accountUuid = null;
        } else {
            try {
                accountUuid = UUID.fromString(accountId);
            } catch (final IllegalArgumentException e) {
                reject(rsp, 400, "Invalid " + ACCOUNT_ID_PARAM + ": not a UUID");
                return;
            }
        }

        final UUID tenantId = tenant == null ? null : tenant.getId();
        String tenantRecordId = null;
        if (tenantId != null) {
            tenantRecordId = recordId(tenantId, ObjectType.TENANT, new PluginTenantContext(null, tenantId));
            if (tenantRecordId == null) {
                rejectUnresolvableTenant(rsp, tenantId);
                return;
            }
        }

        String accountRecordId = null;
        if (accountUuid != null) {
            accountRecordId = recordId(accountUuid, ObjectType.ACCOUNT, new PluginTenantContext(accountUuid, tenantId));
        }

        req.set(SseParamsCriteria.REQUEST_KEY, new SseParamsCriteria(tenantRecordId, accountUuid != null, accountRecordId, userToken));
        chain.next(req, rsp);
    }

    // A tenant validated by TenantFilter must have a record id. If not, this is a server-side problem (not the client's),
    // and continuing without tenant scoping would silently expose the entries of all tenants. Fail closed: answer 500
    // before the stream opens, which also closes the connection.
    private void rejectUnresolvableTenant(final Response rsp, final UUID tenantId) throws Throwable {
        logger.error("Unable to resolve the record id of validated tenant {}, rejecting the log stream request", tenantId);
        reject(rsp, 500, "Unable to resolve the tenant");
    }

    // Returns null when the object is unknown (or belongs to another tenant)
    private String recordId(final UUID objectId, final ObjectType objectType, final TenantContext context) {
        final Long recordId = recordIdApi.getRecordId(objectId, objectType, context);
        if (recordId == null || recordId <= 0L) {
            logger.debug("No record id for {} {}", objectType, objectId);
            return null;
        }
        return recordId.toString();
    }

    private static List<String> values(final Request req, final String name) {
        final Mutant param = req.param(name);
        return param.isSet() ? param.toList(String.class) : List.of();
    }

    // A bare (?p), empty (?p=) or whitespace-only value means "not provided"
    private static String nonBlank(final List<String> values) {
        final Optional<String> first = values.stream().filter(v -> v != null && !v.isBlank()).findFirst();
        return first.orElse(null);
    }

    private static void reject(final Response rsp, final int status, final String message) throws Throwable {
        rsp.status(status);
        rsp.send(message);
    }
}
