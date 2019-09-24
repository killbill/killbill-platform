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

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jooby.Request;
import org.jooby.Sse;
import org.jooby.funzy.Throwing;
import org.killbill.commons.concurrent.Executors;

public class LogsSseHandler implements Sse.Handler {

    private final LogEntriesManager logEntriesManager;
    private final ScheduledExecutorService scheduledExecutorService;

    public LogsSseHandler(final LogEntriesManager logEntriesManager) {
        this.logEntriesManager = logEntriesManager;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor("LogsSseHandler");
    }

    @Override
    public void handle(final Request req, final Sse sse) {
        final UUID cacheId = UUID.fromString(sse.id());
        logEntriesManager.subscribe(cacheId);

        final ScheduledFuture<?> future = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (final LogEntryJson logEntryJson : logEntriesManager.drain(cacheId)) {
                    sse.event(logEntryJson).id(logEntryJson.getId()).send();
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
