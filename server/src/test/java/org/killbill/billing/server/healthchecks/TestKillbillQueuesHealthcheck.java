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

package org.killbill.billing.server.healthchecks;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.killbill.billing.server.config.KillbillServerConfig;
import org.killbill.billing.server.healthchecks.KillbillQueuesHealthcheck.QueueStats;
import org.killbill.bus.api.PersistentBus;
import org.killbill.clock.ClockMock;
import org.killbill.commons.health.api.Result;
import org.killbill.notificationq.api.NotificationQueueService;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestKillbillQueuesHealthcheck {

    private static final double DELTA = 0.0001;

    private KillbillQueuesHealthcheck healthcheck;
    private ClockMock clock;
    private AtomicLong currentBusEntries;

    @BeforeMethod(groups = "fast")
    public void setUp() throws Exception {
        final NotificationQueueService notificationQueueService = Mockito.mock(NotificationQueueService.class);
        Mockito.when(notificationQueueService.getNotificationQueues()).thenReturn(Collections.emptyList());

        final PersistentBus externalBus = Mockito.mock(PersistentBus.class);
        Mockito.when(externalBus.getNbReadyEntries(Mockito.any(DateTime.class))).thenThrow(UnsupportedOperationException.class);

        currentBusEntries = new AtomicLong(0);
        final PersistentBus bus = Mockito.mock(PersistentBus.class);
        Mockito.when(bus.toString()).thenReturn("internalBus");
        Mockito.when(bus.getNbReadyEntries(Mockito.any(DateTime.class))).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(final InvocationOnMock invocation) throws Throwable {
                return currentBusEntries.get();
            }
        });

        clock = new ClockMock();

        final KillbillServerConfig config = Mockito.mock(KillbillServerConfig.class);


        healthcheck = new KillbillQueuesHealthcheck(clock,
                                                    notificationQueueService,
                                                    bus,
                                                    config,
                                                    externalBus);
        healthcheck.activateHealthcheck();
    }

    @Test(groups = "fast")
    public void testUpdateRegression() throws Exception {
        final QueueStats queueStats = new QueueStats("myQ", 3, 0.3);

        queueStats.record(0L, 0L);
        Assert.assertTrue(Arrays.deepEquals(queueStats.getTimestamps().toArray(), new Long[]{0L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getRawSizes().toArray(), new Long[]{0L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getSmoothedSizes().toArray(), new Double[]{0.0}));
        Assert.assertFalse(queueStats.isGrowing());

        queueStats.record(1L, 10L);
        Assert.assertTrue(Arrays.deepEquals(queueStats.getTimestamps().toArray(), new Long[]{0L, 1L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getRawSizes().toArray(), new Long[]{0L, 10L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getSmoothedSizes().toArray(), new Double[]{0.0, 3.0}));
        Assert.assertFalse(queueStats.isGrowing());

        queueStats.record(2L, 15L);
        Assert.assertTrue(Arrays.deepEquals(queueStats.getTimestamps().toArray(), new Long[]{0L, 1L, 2L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getRawSizes().toArray(), new Long[]{0L, 10L, 15L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getSmoothedSizes().toArray(), new Double[]{0.0, 3.0, 6.6}));
        Assert.assertTrue(queueStats.isGrowing());

        queueStats.record(3L, 9L);
        Assert.assertTrue(Arrays.deepEquals(queueStats.getTimestamps().toArray(), new Long[]{1L, 2L, 3L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getRawSizes().toArray(), new Long[]{10L, 15L, 9L}));
        Assert.assertEquals((Double) queueStats.getSmoothedSizes().toArray()[2], 7.32, DELTA);
        Assert.assertTrue(queueStats.isGrowing());

        queueStats.record(4L, 12L);
        Assert.assertTrue(Arrays.deepEquals(queueStats.getTimestamps().toArray(), new Long[]{2L, 3L, 4L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getRawSizes().toArray(), new Long[]{15L, 9L, 12L}));
        Assert.assertEquals((Double) queueStats.getSmoothedSizes().toArray()[2], 8.724, DELTA);
        Assert.assertTrue(queueStats.isGrowing());

        queueStats.record(5L, 14L);
        Assert.assertTrue(Arrays.deepEquals(queueStats.getTimestamps().toArray(), new Long[]{3L, 4L, 5L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getRawSizes().toArray(), new Long[]{9L, 12L, 14L}));
        Assert.assertEquals((Double) queueStats.getSmoothedSizes().toArray()[2], 10.3068, DELTA);
        Assert.assertTrue(queueStats.isGrowing());

        queueStats.record(6L, 18L);
        Assert.assertTrue(Arrays.deepEquals(queueStats.getTimestamps().toArray(), new Long[]{4L, 5L, 6L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getRawSizes().toArray(), new Long[]{12L, 14L, 18L}));
        Assert.assertEquals((Double) queueStats.getSmoothedSizes().toArray()[2], 12.6148, DELTA);
        Assert.assertTrue(queueStats.isGrowing());

        queueStats.record(7L, 0L);
        Assert.assertTrue(Arrays.deepEquals(queueStats.getTimestamps().toArray(), new Long[]{5L, 6L, 7L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getRawSizes().toArray(), new Long[]{14L, 18L, 0L}));
        Assert.assertEquals((Double) queueStats.getSmoothedSizes().toArray()[2], 8.8303, DELTA);
        Assert.assertFalse(queueStats.isGrowing());

        queueStats.record(8L, 0L);
        Assert.assertTrue(Arrays.deepEquals(queueStats.getTimestamps().toArray(), new Long[]{6L, 7L, 8L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getRawSizes().toArray(), new Long[]{18L, 0L, 0L}));
        Assert.assertEquals((Double) queueStats.getSmoothedSizes().toArray()[2], 6.1812, DELTA);
        Assert.assertFalse(queueStats.isGrowing());

        queueStats.record(9L, 0L);
        Assert.assertTrue(Arrays.deepEquals(queueStats.getTimestamps().toArray(), new Long[]{7L, 8L, 9L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getRawSizes().toArray(), new Long[]{0L, 0L, 0L}));
        Assert.assertEquals((Double) queueStats.getSmoothedSizes().toArray()[2], 4.3268, DELTA);
        Assert.assertFalse(queueStats.isGrowing());

        queueStats.record(10L, 0L);
        Assert.assertTrue(Arrays.deepEquals(queueStats.getTimestamps().toArray(), new Long[]{8L, 9L, 10L}));
        Assert.assertTrue(Arrays.deepEquals(queueStats.getRawSizes().toArray(), new Long[]{0L, 0L, 0L}));
        Assert.assertEquals((Double) queueStats.getSmoothedSizes().toArray()[2], 3.0288, DELTA);
        Assert.assertFalse(queueStats.isGrowing());
    }

    @Test(groups = "fast")
    public void testHealthyGaussian() throws Exception {
        checkResult(0, true);
        checkResult(100, true);
        checkResult(200, true);
        checkResult(500, true);
        checkResult(1000, true);
        checkResult(2500, true);
        checkResult(3500, true);
        checkResult(2000, true);
        checkResult(600, true);
        checkResult(200, true);
        checkResult(50, true);
        // Window size reached
        checkResult(0, true);
        checkResult(100, true);
        checkResult(200, true);
        checkResult(500, true);
        checkResult(1000, true);
        checkResult(2500, true);
        checkResult(3500, true);
        checkResult(2000, true);
        checkResult(600, true);
        checkResult(200, true);
        checkResult(50, true);
    }

    @Test(groups = "fast")
    public void testHealthySinusoid() throws Exception {
        checkResult(0, true);
        checkResult(100, true);
        checkResult(200, true);
        checkResult(280, true);
        checkResult(120, true);
        checkResult(250, true);
        checkResult(280, true);
        checkResult(100, true);
        checkResult(110, true);
        checkResult(150, true);
        checkResult(250, true);
        // Window size reached
        checkResult(200, true);
        checkResult(280, true);
        checkResult(100, true);
        checkResult(110, true);
        checkResult(150, true);
        checkResult(250, true);
        checkResult(280, true);
        checkResult(100, true);
        checkResult(110, true);
        checkResult(150, true);
        checkResult(250, true);
    }

    @Test(groups = "fast")
    public void testUnHealthyGrowingQueue() throws Exception {
        checkResult(0, true);
        checkResult(100, true);
        checkResult(200, true);
        checkResult(500, true);
        checkResult(1000, true);
        checkResult(2500, true);
        checkResult(3500, true);
        checkResult(4500, true);
        checkResult(6000, true);
        checkResult(6100, true);
        checkResult(6200, true);
        // Window size reached
        checkResult(6300, false);
        checkResult(9300, false);
        checkResult(16300, false);
    }

    private void checkResult(final int newBusEntries, final boolean healthy) {
        clock.addDeltaFromReality(Period.minutes(5).toStandardDuration().getMillis());
        currentBusEntries.set(newBusEntries);

        final Result result = healthcheck.check(12, 0.3);
        Assert.assertEquals(result.isHealthy(), healthy, result.toString());
    }
}
