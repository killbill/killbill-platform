/*
 * Copyright 2014-2019 Groupon, Inc
 * Copyright 2014-2019 The Billing Project, LLC
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
import org.killbill.commons.concurrent.Executors;

public class LogsSseHandler implements Sse.Handler, Closeable {

    private final LogEntriesManager logEntriesManager;
    private final ScheduledExecutorService scheduledExecutorService;

    public LogsSseHandler(final LogEntriesManager logEntriesManager) {
        this.logEntriesManager = logEntriesManager;
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

        final AtomicReference<UUID> lastLogId = new AtomicReference<UUID>();
        final ScheduledFuture<?> future = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                final Iterable<LogEntryJson> logEntries = logEntriesManager.drain(cacheId);
                if (!logEntries.iterator().hasNext()) {
                    // In case we have nothing to send, send a heartbeat to verify the client is still around
                    // That way, we can more quickly cleanup our subscriptions
                    // Note that we set the id as the last log id, so that we can easily resume
                    sse.event("heartbeat").id(lastLogId.get()).send();
                } else {
                    for (final LogEntryJson logEntryJson : logEntries) {
                        sse.event(logEntryJson).id(logEntryJson.getId()).send();
                        lastLogId.set(logEntryJson.getId());
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);

        sse.onClose(new Throwing.Runnable() {
            @Override
            public void tryRun() throws Throwable {
                future.cancel(true);
                logEntriesManager.unsubscribe(cacheId);
            }
        });
    }
}
