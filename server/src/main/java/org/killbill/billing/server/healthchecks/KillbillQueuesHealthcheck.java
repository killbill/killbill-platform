/*
 * Copyright 2014-2017 Groupon, Inc
 * Copyright 2014-2017 The Billing Project, LLC
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import com.codahale.metrics.health.annotation.Async.InitialState;
import com.codahale.metrics.health.annotation.Async.ScheduleType;
import com.google.common.annotations.VisibleForTesting;

// Run this check asynchronously as it executes database queries: when the healthcheck is integrated with a load balancer,
// we don't want to DDOS the database as the polling interval is most likely in the order of a few seconds (or less).
// Note: when the queues are configured in a sticky mode (e.g. on premise deployment), if this check fails, it means that
// particular node in overloaded (cannot keep up processing bus or notification entries). Taking it out of rotation for a bit
// makes sense, so it catches up before processing new requests. When the queues are configured in a polling mode however
// (e.g. cloud deployment), all nodes behave the same (the healthcheck will fail on all nodes at the same time): in that case,
// instead of taking the nodes out of rotation, new nodes should be deployed instead (i.e. Auto Scaling should be enabled).
@Async(initialState = InitialState.HEALTHY, initialDelay = 5, period = 5, unit = TimeUnit.MINUTES, scheduleType = ScheduleType.FIXED_DELAY)
@Singleton
public class KillbillQueuesHealthcheck extends HealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(KillbillQueuesHealthcheck.class);

    // Only consider the last 12 data points (60 minutes) to compute whether the queues are growing
    private static final int SLIDING_WINDOW_SIZE = 12;

    @VisibleForTesting
    final Map<String, double[][]> reservoir = new HashMap<String, double[][]>();
    private final Map<String, SimpleRegression> regressionPerQueue = new HashMap<String, SimpleRegression>();

    private final Clock clock;
    private final Collection<PersistentBus> buses = new HashSet<PersistentBus>();
    private final NotificationQueueService notificationQueueService;

    @Inject
    public KillbillQueuesHealthcheck(final Clock clock,
                                     final NotificationQueueService notificationQueueService,
                                     final PersistentBus bus,
                                     @Named("externalBus") final PersistentBus externalBus) {
        this.clock = clock;
        this.notificationQueueService = notificationQueueService;
        this.buses.add(bus);
        this.buses.add(externalBus);
    }

    @Override
    public Result check() {
        final Map<String, Double> slopePerQueueIds = new HashMap<String, Double>();
        final DateTime now = clock.getUTCNow();

        for (final PersistentBus bus : buses) {
            final String persistentBusId = bus.toString();
            try {
                final long nbReadyEntries = bus.getNbReadyEntries(now);
                slopePerQueueIds.put(persistentBusId, updateRegression(persistentBusId, now.getMillis(), nbReadyEntries, SLIDING_WINDOW_SIZE));
            } catch (final UnsupportedOperationException e) {
                // Ignore - not supported by this queue
            }
        }

        for (final NotificationQueue notificationQueue : notificationQueueService.getNotificationQueues()) {
            final String notificationQueueId = notificationQueue.toString();

            try {
                final long nbReadyEntries = notificationQueue.getNbReadyEntries(now);
                slopePerQueueIds.put(notificationQueueId, updateRegression(notificationQueueId, now.getMillis(), nbReadyEntries, SLIDING_WINDOW_SIZE));
            } catch (final UnsupportedOperationException e) {
                // Ignore - not supported by this queue
            }
        }

        return buildHealthcheckResponse(slopePerQueueIds);
    }

    @VisibleForTesting
    double updateRegression(final String queueId, final long now, final long nbReadyEntries, final int slidingWindowSize) {
        initMaps(queueId, slidingWindowSize);
        final SimpleRegression regression = regressionPerQueue.get(queueId);
        final double[][] reservoir = this.reservoir.get(queueId);

        // Remove the oldest data point from the regression
        if (regression.getN() >= slidingWindowSize) {
            regression.removeData(reservoir[0][0], reservoir[0][1]);
        }

        // Update the regression with the latest data point
        regression.addData(now, nbReadyEntries);
        final double slope = regression.getSlope();

        // Advance our sliding window
        for (int i = 0; i < slidingWindowSize - 1; i++) {
            reservoir[i][0] = reservoir[i + 1][0];
            reservoir[i][1] = reservoir[i + 1][1];
        }
        reservoir[slidingWindowSize - 1][0] = now;
        reservoir[slidingWindowSize - 1][1] = nbReadyEntries;

        return Double.isNaN(slope) ? 0 : slope;
    }

    private void initMaps(final String queueId, final int slidingWindowSize) {
        if (regressionPerQueue.get(queueId) == null) {
            regressionPerQueue.put(queueId, new SimpleRegression(true));
        }
        if (reservoir.get(queueId) == null) {
            reservoir.put(queueId, new double[slidingWindowSize][2]);
        }
    }

    private Result buildHealthcheckResponse(final Map<String, Double> slopePerQueueIds) {
        final ResultBuilder resultBuilder = Result.builder();

        final StringBuilder stringBuilderForMessage = new StringBuilder("Growing queues: ");
        boolean healthy = true;
        int i = 0;
        for (final String growingQueueId : slopePerQueueIds.keySet()) {
            final double slope = Math.round(slopePerQueueIds.get(growingQueueId) * 100);
            if (slope > 0) {
                healthy = false;

                if (i > 0) {
                    stringBuilderForMessage.append(", ");
                }
                i++;

                stringBuilderForMessage.append(growingQueueId)
                                       .append(" (")
                                       .append(slope)
                                       .append("%)");
            }

            // Display the reservoir, regardless of the health status
            final StringBuilder stringBuilderForDetail = new StringBuilder();
            final double[][] queueReservoir = this.reservoir.get(growingQueueId);
            for (int j = 0; j < queueReservoir.length; j++) {
                if (j > 0) {
                    stringBuilderForDetail.append(",");
                }
                stringBuilderForDetail.append(queueReservoir[j][1]);
            }
            resultBuilder.withDetail(String.format("%s historical queue size", growingQueueId), stringBuilderForDetail.toString());
        }

        if (healthy) {
            resultBuilder.healthy();
        } else {
            resultBuilder.unhealthy()
                         .withMessage(stringBuilderForMessage.toString());
        }

        return resultBuilder.build();
    }
}
