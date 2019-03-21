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

package org.killbill.billing.server.healthchecks;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.DateTime;
import org.killbill.bus.api.PersistentBus;
import org.killbill.clock.Clock;
import org.killbill.notificationq.api.NotificationQueue;
import org.killbill.notificationq.api.NotificationQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weakref.jmx.Managed;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import com.codahale.metrics.health.annotation.Async.InitialState;
import com.codahale.metrics.health.annotation.Async.ScheduleType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.EvictingQueue;

// Run this check asynchronously as it executes database queries: when the healthcheck is integrated with a load balancer,
// we don't want to DDOS the database as the polling interval is most likely in the order of a few seconds (or less).
// Note: when the queues are configured in a sticky mode (e.g. on premise deployment), if this check fails, it means that
// particular node is overloaded (cannot keep up processing bus or notification entries). Taking it out of rotation for a bit
// makes sense, so it catches up before processing new requests. When the queues are configured in a polling mode however
// (e.g. cloud deployment), all nodes behave the same (the healthcheck will fail on all nodes at the same time): in that case,
// instead of taking the nodes out of rotation, new nodes should be deployed instead (i.e. Auto Scaling should be enabled), provided
// the database is able to sustain the additional load.
@Async(initialState = InitialState.HEALTHY, initialDelay = 0, period = 1, unit = TimeUnit.MINUTES, scheduleType = ScheduleType.FIXED_DELAY)
@Singleton
public class KillbillQueuesHealthcheck extends HealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(KillbillQueuesHealthcheck.class);

    // Only consider the last 60 data points (60 minutes) to compute whether the queues are growing
    private static final int SLIDING_WINDOW_SIZE = 60;
    // Simple exponential smoothing factor
    private static final double ALPHA = 0.3;

    private final Map<String, QueueStats> statsPerQueue = new HashMap<String, QueueStats>();

    private final AtomicBoolean healthcheckActive = new AtomicBoolean(false);

    private final Clock clock;
    private final PersistentBus bus;
    private final PersistentBus externalBus;
    private final NotificationQueueService notificationQueueService;

    @Inject
    public KillbillQueuesHealthcheck(final Clock clock,
                                     final NotificationQueueService notificationQueueService,
                                     final PersistentBus bus,
                                     @Named("externalBus") final PersistentBus externalBus) {
        this.clock = clock;
        this.notificationQueueService = notificationQueueService;
        this.bus = bus;
        this.externalBus = externalBus;
    }

    @Managed(description = "Kill Bill queues healthcheck")
    public boolean isHealthy() {
        final Result result = check();
        logger.info("Queues healthcheck result: {}", result);
        return result.isHealthy();
    }

    @Managed(description = "Deactivate healthcheck")
    public void deactivateHealthcheck() {
        logger.warn("Deactivating healthcheck: queues results will be ignored");
        healthcheckActive.set(false);
    }

    @Managed(description = "Activate healthcheck")
    public void activateHealthcheck() {
        logger.warn("Activating healthcheck: queues results will be NOT be ignored");
        healthcheckActive.set(true);
    }

    @Override
    public Result check() {
        return check(SLIDING_WINDOW_SIZE, ALPHA);
    }

    @VisibleForTesting
    Result check(final int slidingWindowSize, final double alpha) {
        final DateTime now = clock.getUTCNow();

        if (bus != null) {
            try {
                final long nbReadyEntries = bus.getNbReadyEntries(now);
                updateRegression("bus", now.getMillis(), nbReadyEntries, slidingWindowSize, alpha);
            } catch (final UnsupportedOperationException e) {
                // Ignore - not supported by this queue
            }
        }

        if (externalBus != null) {
            try {
                final long nbReadyEntries = externalBus.getNbReadyEntries(now);
                updateRegression("externalBus", now.getMillis(), nbReadyEntries, slidingWindowSize, alpha);
            } catch (final UnsupportedOperationException e) {
                // Ignore - not supported by this queue
            }
        }

        for (final NotificationQueue notificationQueue : notificationQueueService.getNotificationQueues()) {
            final String notificationQueueId = notificationQueue.getFullQName();

            try {
                final long nbReadyEntries = notificationQueue.getNbReadyEntries(now);
                updateRegression(notificationQueueId, now.getMillis(), nbReadyEntries, slidingWindowSize, alpha);
            } catch (final UnsupportedOperationException e) {
                // Ignore - not supported by this queue
            }
        }

        final Result healthcheckResponse = buildHealthcheckResponse();

        for (final Object queueStatsObject : healthcheckResponse.getDetails().values()) {
            final QueueStats queueStats = (QueueStats) queueStatsObject;

            logger.debug("healthy='{}', message='{}', error='{}', queue='{}', rawSize='{}', smoothedSize='{}', smoothedSizeSlope='{}'",
                         healthcheckResponse.isHealthy(),
                         healthcheckResponse.getMessage(),
                         healthcheckResponse.getError(),
                         queueStats.queueId,
                         queueStats.lastRawSize,
                         queueStats.lastSmoothedSize,
                         queueStats.currentSmoothedSizesSlope);
        }
        return healthcheckResponse;
    }

    private void updateRegression(final String queueId, final long now, final long nbReadyEntries, final int slidingWindowSize, final double alpha) {
        if (statsPerQueue.get(queueId) == null) {
            statsPerQueue.put(queueId, new QueueStats(queueId, slidingWindowSize, alpha));
        }

        statsPerQueue.get(queueId).record(now, nbReadyEntries);
    }

    private Result buildHealthcheckResponse() {
        final ResultBuilder resultBuilder = Result.builder();

        final StringBuilder stringBuilderForMessage = new StringBuilder("Growing queues: ");
        boolean healthy = true;
        int i = 0;

        for (final String growingQueueId : statsPerQueue.keySet()) {
            final QueueStats queueStats = statsPerQueue.get(growingQueueId);
            if (queueStats.isGrowing()) {
                healthy = false;

                if (i > 0) {
                    stringBuilderForMessage.append(", ");
                }
                i++;

                stringBuilderForMessage.append(growingQueueId)
                                       .append(" (")
                                       .append(queueStats.currentSmoothedSizesSlope)
                                       .append(")");
            }

            // Display the stats, regardless of the health status
            resultBuilder.withDetail(growingQueueId, queueStats);
        }

        if (healthy || !healthcheckActive.get()) {
            resultBuilder.healthy();
        } else {
            resultBuilder.unhealthy()
                         .withMessage(stringBuilderForMessage.toString());
        }

        return resultBuilder.build();
    }

    @VisibleForTesting
    static final class QueueStats {

        private final String queueId;
        // Number of samples to consider for our sliding window
        private final double slidingWindowSize;
        // X axis: timestamps
        private final EvictingQueue<Long> timestamps;
        // Y axis: sizes measured
        private final EvictingQueue<Long> rawSizes;
        // Y axis: exponential moving average of the sizes measured
        private final EvictingQueue<Double> smoothedSizes;
        private final SimpleRegression smoothedSizesRegression;
        private final HoltWintersComputer holtWintersComputer;
        // Linear regression to check for current trend over the slidingWindowSize
        private Double currentSmoothedSizesSlope = 0.0;

        private Long lastRawSize;
        private double lastSmoothedSize;

        public QueueStats(final String queueId, final int slidingWindowSize, final double alpha) {
            this.queueId = queueId;
            this.slidingWindowSize = slidingWindowSize;
            this.timestamps = EvictingQueue.<Long>create(slidingWindowSize);
            this.rawSizes = EvictingQueue.<Long>create(slidingWindowSize);
            this.smoothedSizes = EvictingQueue.<Double>create(slidingWindowSize);

            this.smoothedSizesRegression = new SimpleRegression(true);
            this.holtWintersComputer = new HoltWintersComputer(alpha);
        }

        public void record(final long newestTimestamp, final long newestRawSize) {
            // Remove the oldest data point from the regression (the regression is only applied to the sliding window of observations)
            if (smoothedSizesRegression.getN() >= slidingWindowSize) {
                final Long oldestTimestamp = timestamps.peek();
                final Double oldestSmoothedSize = smoothedSizes.peek();
                smoothedSizesRegression.removeData(oldestTimestamp, oldestSmoothedSize);
            }

            // Compute the next smoothed value to filter out noise
            holtWintersComputer.addNextValue(newestRawSize);
            // Note: "1" here is ignored
            final double newestSmoothedSize = holtWintersComputer.getForecast(1);

            // Update the regression with the latest smoothed data point
            smoothedSizesRegression.addData(newestTimestamp, newestSmoothedSize);
            // Wait until we have enough data
            if (smoothedSizesRegression.getN() >= slidingWindowSize) {
                final double rawSmoothedSlope = smoothedSizesRegression.getSlope();
                currentSmoothedSizesSlope = Double.isNaN(rawSmoothedSlope) ? 0 : new BigDecimal(rawSmoothedSlope * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            }

            // Store the new values
            timestamps.add(newestTimestamp);
            rawSizes.add(newestRawSize);
            smoothedSizes.add(newestSmoothedSize);

            lastRawSize = newestRawSize;
            lastSmoothedSize = newestSmoothedSize;
        }

        // The slope of the smoothed observations gives us the overall trend over the slidingWindowSize
        public boolean isGrowing() {
            return currentSmoothedSizesSlope > 0.1;
        }

        @VisibleForTesting
        EvictingQueue<Long> getTimestamps() {
            return timestamps;
        }

        @VisibleForTesting
        EvictingQueue<Long> getRawSizes() {
            return rawSizes;
        }

        @VisibleForTesting
        EvictingQueue<Double> getSmoothedSizes() {
            return smoothedSizes;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("QueueStats{");
            sb.append("queueId='").append(queueId).append('\'');
            sb.append(", slidingWindowSize=").append(slidingWindowSize);
            sb.append(", timestamps=").append(timestamps);
            sb.append(", rawSizes=").append(rawSizes);
            sb.append(", smoothedSizes=").append(smoothedSizes);
            sb.append(", currentSmoothedSizesSlope=").append(currentSmoothedSizesSlope).append("%");
            sb.append('}');
            return sb.toString();
        }
    }
}
