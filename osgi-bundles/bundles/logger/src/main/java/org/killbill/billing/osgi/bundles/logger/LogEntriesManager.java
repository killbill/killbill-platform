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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.killbill.commons.utils.collect.EvictingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class LogEntriesManager implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(LogEntriesManager.class);

    private final Map<UUID, EvictingQueue<LogEntryJson>> sseIDsCaches;
    private final EvictingQueue<LogEntryJson> rootCache;

    public LogEntriesManager() {
        sseIDsCaches = new HashMap<>();
        final UUID rootCacheId = UUID.randomUUID();
        rootCache = subscribe(rootCacheId, null);
    }

    public void recordEvent(final LogEntryJson logEntry) {
        synchronized (sseIDsCaches) {
            for (final EvictingQueue<LogEntryJson> cache : sseIDsCaches.values()) {
                cache.add(logEntry);
            }
        }
    }

    public EvictingQueue<LogEntryJson> subscribe(final UUID cacheId, @Nullable final UUID lastEventId) {
        final EvictingQueue<LogEntryJson> cache = new EvictingQueue<>(500);
        synchronized (sseIDsCaches) {
            sseIDsCaches.put(cacheId, cache);
            if (rootCache != null) {
                if (lastEventId == null) {
                    // Add all entries
                    cache.addAll(rootCache);
                } else {
                    for (final LogEntryJson logEntryJson : rootCache) {
                        if (lastEventId.equals(logEntryJson.getId())) {
                            // Remove everything prior to that id
                            cache.clear();
                            continue;
                        }
                        cache.add(logEntryJson);
                    }
                }
            }
        }
        // Logging should be done outside the synchronized block to avoid any deadlock (the log entry will be put in the caches)
        logger.info("Created new cache {} ({} active)", cacheId, sseIDsCaches.size());
        return cache;
    }

    public void unsubscribe(final UUID cacheId) {
        final EvictingQueue<LogEntryJson> cache;
        synchronized (sseIDsCaches) {
            cache = sseIDsCaches.remove(cacheId);
        }
        // Logging should be done outside the synchronized block to avoid any deadlock (the log entry will be put in the caches)
        logger.info("Removed cache {} ({} active)", cacheId, sseIDsCaches.size());
        cache.clear();
    }

    public Iterable<LogEntryJson> drain(final UUID cacheId) {
        final Collection<LogEntryJson> elements = new LinkedList<LogEntryJson>();

        synchronized (sseIDsCaches) {
            final EvictingQueue<LogEntryJson> logEntries = sseIDsCaches.get(cacheId);
            LogEntryJson logEntry = logEntries.poll();
            while (logEntry != null) {
                elements.add(logEntry);
                logEntry = logEntries.poll();
            }
            return elements;
        }
    }

    @Override
    public void close() {
        synchronized (sseIDsCaches) {
            for (final EvictingQueue<LogEntryJson> cache : sseIDsCaches.values()) {
                cache.clear();
            }
            sseIDsCaches.clear();
        }
    }
}
