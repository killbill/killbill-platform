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
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestSseParamsCriteria {

    private static LogEntryJson entry(final String tenant, final String account, final String token) {
        return new LogEntryJson(null, 3, "logger", "msg", token, tenant, account, null);
    }

    private static List<LogEntryJson> apply(final SseParamsCriteria filter, final LogEntryJson... entries) {
        return StreamSupport.stream(filter.apply(List.of(entries)).spliterator(), false).toList();
    }

    @Test(groups = "fast")
    public void testNoFilterKeepsEverythingIncludingEntriesWithoutIds() {
        Assert.assertFalse(SseParamsCriteria.NONE.isActive());
        final LogEntryJson noIds = entry(null, null, null);
        Assert.assertEquals(apply(SseParamsCriteria.NONE, noIds, entry("1", "2", "t")).size(), 2);
    }

    @Test(groups = "fast")
    public void testTenantScoping() {
        final SseParamsCriteria filter = new SseParamsCriteria("1", false, null, null);
        final LogEntryJson mine = entry("1", null, null);
        Assert.assertEquals(apply(filter, mine, entry("2", null, null), entry(null, null, null)), List.of(mine));
    }

    @Test(groups = "fast")
    public void testAccountMatchesExactRecordId() {
        final SseParamsCriteria filter = new SseParamsCriteria("1", true, "10", null);
        final LogEntryJson mine = entry("1", "10", null);
        // "100" must not match "10", an entry without account is excluded, and so is the same account id in another tenant
        Assert.assertEquals(apply(filter, mine, entry("1", "100", null), entry("1", null, null), entry("2", "10", null)), List.of(mine));
    }

    @Test(groups = "fast")
    public void testUnknownAccountMatchesNothing() {
        final SseParamsCriteria filter = new SseParamsCriteria("1", true, null, null);
        Assert.assertTrue(filter.isActive());
        Assert.assertTrue(apply(filter, entry("1", "10", null), entry("1", null, null)).isEmpty());
    }

    @Test(groups = "fast")
    public void testUserTokenAlone() {
        final String token = UUID.randomUUID().toString();
        final SseParamsCriteria filter = new SseParamsCriteria(null, false, null, token);
        final LogEntryJson mine = entry("7", null, token);
        Assert.assertEquals(apply(filter, mine, entry("7", null, "other"), entry("7", null, null)), List.of(mine));
    }

    @Test(groups = "fast")
    public void testFiltersCombineWithAnd() {
        final SseParamsCriteria filter = new SseParamsCriteria("1", true, "10", "tok");
        final LogEntryJson mine = entry("1", "10", "tok");
        Assert.assertEquals(apply(filter, mine, entry("1", "10", "x"), entry("1", "11", "tok"), entry("2", "10", "tok")), List.of(mine));
    }
}
