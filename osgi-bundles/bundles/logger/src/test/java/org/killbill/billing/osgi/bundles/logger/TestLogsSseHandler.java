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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jooby.Request;
import org.jooby.Sse;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class TestLogsSseHandler {

    private static final int TIMEOUT_MS = 5000;

    @Test(groups = "fast")
    public void testHeartbeatWithoutIdWhenNothingWasSentYet() throws Exception {
        final LogsSseHandler handler = new LogsSseHandler(new LogEntriesManager());
        try {
            // Active criteria that match nothing: only heartbeats can be sent, and no entry was ever sent, so there is no last id.
            // Sse.Event#id rejects null, and the resulting exception would silently cancel the periodic task.
            final SseParamsCriteria matchesNothing = new SseParamsCriteria(null, false, null, "no-such-token");
            final Sse sse = mockSse(UUID.randomUUID());
            final Sse.Event heartbeat = mockEvent();
            Mockito.when(sse.event("heartbeat")).thenReturn(heartbeat);

            handler.handle(mockRequest(matchesNothing), sse);

            // Heartbeats keep coming: the periodic task was not cancelled by an exception
            Mockito.verify(heartbeat, Mockito.timeout(TIMEOUT_MS).atLeast(2)).send();
            Mockito.verify(heartbeat, Mockito.never()).id(Mockito.any());
        } finally {
            handler.close();
        }
    }

    @Test(groups = "fast")
    public void testHeartbeatCarriesTheLastSentEntryId() throws Exception {
        final LogEntriesManager logEntriesManager = new LogEntriesManager();
        final LogsSseHandler handler = new LogsSseHandler(logEntriesManager);
        try {
            final LogEntryJson matching = new LogEntryJson(null, 3, "logger", "msg", "tok", "1", "2", null);
            logEntriesManager.recordEvent(matching);
            logEntriesManager.recordEvent(new LogEntryJson(null, 3, "logger", "msg", "other", "1", "2", null));

            final Sse sse = mockSse(UUID.randomUUID());
            final Sse.Event entryEvent = mockEvent();
            final Sse.Event heartbeat = mockEvent();
            Mockito.when(sse.event("heartbeat")).thenReturn(heartbeat);
            Mockito.when(sse.event(Mockito.any(LogEntryJson.class))).thenReturn(entryEvent);

            handler.handle(mockRequest(new SseParamsCriteria(null, false, null, "tok")), sse);

            // Only the matching entry is sent, then heartbeats resume from its id
            Mockito.verify(heartbeat, Mockito.timeout(TIMEOUT_MS).atLeastOnce()).id(matching.getId());
            final ArgumentCaptor<LogEntryJson> sent = ArgumentCaptor.forClass(LogEntryJson.class);
            Mockito.verify(sse, Mockito.atLeastOnce()).event(sent.capture());
            sent.getAllValues().forEach(e -> org.testng.Assert.assertEquals(e.getUserToken(), "tok"));
            Mockito.verify(entryEvent, Mockito.times(1)).send();
        } finally {
            handler.close();
        }
    }

    @Test(groups = "fast")
    public void testNoFilterOnTheRequestStreamsEverything() throws Exception {
        final LogEntriesManager logEntriesManager = new LogEntriesManager();
        final LogsSseHandler handler = new LogsSseHandler(logEntriesManager);
        try {
            logEntriesManager.recordEvent(new LogEntryJson(null, 3, "logger", "msg", null, null, null, null));
            final Sse sse = mockSse(UUID.randomUUID());
            final Sse.Event entryEvent = mockEvent();
            Mockito.when(sse.event(Mockito.any(LogEntryJson.class))).thenReturn(entryEvent);
            final Request req = Mockito.mock(Request.class);
            Mockito.when(req.ifGet(SseParamsCriteria.REQUEST_KEY)).thenReturn(Optional.empty());

            handler.handle(req, sse);

            Mockito.verify(entryEvent, Mockito.timeout(TIMEOUT_MS).times(1)).send();
        } finally {
            handler.close();
        }
    }

    private static Request mockRequest(final SseParamsCriteria criteria) {
        final Request req = Mockito.mock(Request.class);
        Mockito.when(req.ifGet(SseParamsCriteria.REQUEST_KEY)).thenReturn(Optional.of(criteria));
        return req;
    }

    private static Sse mockSse(final UUID id) {
        final Sse sse = Mockito.mock(Sse.class);
        Mockito.when(sse.id()).thenReturn(id.toString());
        Mockito.when(sse.lastEventId(UUID.class)).thenReturn(Optional.empty());
        return sse;
    }

    private static Sse.Event mockEvent() {
        final Sse.Event event = Mockito.mock(Sse.Event.class);
        Mockito.when(event.id(Mockito.any())).thenReturn(event);
        Mockito.when(event.send()).thenReturn(new CompletableFuture<>());
        return event;
    }
}
