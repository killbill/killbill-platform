/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jooby.Request;
import org.jooby.Sse;
import org.jooby.funzy.Throwing;
import org.killbill.billing.util.api.RecordIdApi;
import org.killbill.commons.concurrent.Executors;
import org.killbill.commons.utils.collect.Iterables;

public class LogsSseHandler implements Sse.Handler, Closeable {

    private static final long INITIAL_DELAY_MS = 200;
    private static final long PERIOD_MS = 1000;

    private final LogEntriesManager logEntriesManager;
    private final RecordIdApi recordIdApi;
    private final ScheduledExecutorService scheduledExecutorService;

    public LogsSseHandler(final LogEntriesManager logEntriesManager, final RecordIdApi recordIdApi) {
        this.logEntriesManager = logEntriesManager;
        this.recordIdApi = recordIdApi;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor("LogsSseHandler");
    }

    @Override
    public void close() throws IOException {
        this.scheduledExecutorService.shutdownNow();
        this.logEntriesManager.close();
    }

    @Override
    public void handle(final Request req, final Sse sse) {
        final UUID lastEventId = sse.lastEventId(UUID.class).orElse(null);
        final UUID cacheId = UUID.fromString(sse.id());
        logEntriesManager.subscribe(cacheId, lastEventId);

        final LogEntriesFilter entriesFilter = new LogEntriesFilter(req, this.recordIdApi);

        final AtomicReference<UUID> lastLogId = new AtomicReference<>();
        final ScheduledFuture<?> future = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                final Iterable<LogEntryJson> logEntries = logEntriesManager.drain(cacheId);
                if (Iterables.isEmpty(logEntries)) {
                    sendHeartbeat();
                } else {
                    for (final LogEntryJson logEntryJson : entriesFilter.apply(logEntries)) {
                        sse.event(logEntryJson).id(logEntryJson.getId()).send();
                        lastLogId.set(logEntryJson.getId());
                    }
                    // Filtered clients may never match any entry. Without a periodic write,
                    // proxies/load balancers may kill idle connections and server won't detect dead peers.
                    if (entriesFilter.hasNoResult()) {
                        sendHeartbeat();
                    }
                }
            }

            void sendHeartbeat() {
                // send heartbeat. ID, if any, is the last log id so resume works correctly.
                final Sse.Event heartbeat = sse.event("heartbeat");
                if (lastLogId.get() != null) {
                    heartbeat.id(lastLogId.get());
                }
                heartbeat.send();
            }
        }, INITIAL_DELAY_MS, PERIOD_MS, TimeUnit.MILLISECONDS);

        sse.onClose(new Throwing.Runnable() {
            @Override
            public void tryRun() throws Throwable {
                future.cancel(true);
                logEntriesManager.unsubscribe(cacheId);
            }
        });
    }
}
