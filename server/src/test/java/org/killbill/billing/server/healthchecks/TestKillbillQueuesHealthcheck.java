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

import java.util.Arrays;

import org.killbill.bus.api.PersistentBus;
import org.killbill.clock.ClockMock;
import org.killbill.notificationq.api.NotificationQueueService;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestKillbillQueuesHealthcheck {

    private static final double DELTA = 0.000000001;

    @Test(groups = "fast")
    public void testUpdateRegression() throws Exception {
        final KillbillQueuesHealthcheck healthcheck = new KillbillQueuesHealthcheck(new ClockMock(),
                                                                                    Mockito.mock(NotificationQueueService.class),
                                                                                    Mockito.mock(PersistentBus.class),
                                                                                    Mockito.mock(PersistentBus.class));

        Assert.assertEquals(healthcheck.updateRegression("myQ", 0L, 0L, 3), 0.0);
        Assert.assertTrue(Arrays.deepEquals(healthcheck.reservoir.get("myQ"), new double[][]{{0.0, 0.0}, {0.0, 0.0}, {0.0, 0.0}}));

        Assert.assertEquals(healthcheck.updateRegression("myQ", 1L, 10L, 3), 10.0);
        Assert.assertTrue(Arrays.deepEquals(healthcheck.reservoir.get("myQ"), new double[][]{{0.0, 0.0}, {0.0, 0.0}, {1.0, 10.0}}));

        Assert.assertEquals(healthcheck.updateRegression("myQ", 2L, 15L, 3), 7.5);
        Assert.assertTrue(Arrays.deepEquals(healthcheck.reservoir.get("myQ"), new double[][]{{0.0, 0.0}, {1.0, 10.0}, {2.0, 15.0}}));

        Assert.assertEquals(healthcheck.updateRegression("myQ", 3L, 9L, 3), -0.5);
        Assert.assertTrue(Arrays.deepEquals(healthcheck.reservoir.get("myQ"), new double[][]{{1.0, 10.0}, {2.0, 15.0}, {3.0, 9.0}}));

        Assert.assertEquals(healthcheck.updateRegression("myQ", 4L, 12L, 3), -1.5, DELTA);
        Assert.assertTrue(Arrays.deepEquals(healthcheck.reservoir.get("myQ"), new double[][]{{2.0, 15.0}, {3.0, 9.0}, {4.0, 12.0}}));

        Assert.assertEquals(healthcheck.updateRegression("myQ", 5L, 14L, 3), 2.5, DELTA);
        Assert.assertTrue(Arrays.deepEquals(healthcheck.reservoir.get("myQ"), new double[][]{{3.0, 9.0}, {4.0, 12.0}, {5.0, 14.0}}));

        Assert.assertEquals(healthcheck.updateRegression("myQ", 6L, 18L, 3), 3.0);
        Assert.assertTrue(Arrays.deepEquals(healthcheck.reservoir.get("myQ"), new double[][]{{4.0, 12.0}, {5.0, 14.0}, {6.0, 18.0}}));

        Assert.assertEquals(healthcheck.updateRegression("myQ", 7L, 0L, 3), -7.0);
        Assert.assertTrue(Arrays.deepEquals(healthcheck.reservoir.get("myQ"), new double[][]{{5.0, 14.0}, {6.0, 18.0}, {7.0, 0.0}}));

        Assert.assertEquals(healthcheck.updateRegression("myQ", 8L, 0L, 3), -9.0, DELTA);
        Assert.assertTrue(Arrays.deepEquals(healthcheck.reservoir.get("myQ"), new double[][]{{6.0, 18.0}, {7.0, 0.0}, {8.0, 0.0}}));

        Assert.assertEquals(healthcheck.updateRegression("myQ", 9L, 0L, 3), 0.0, DELTA);
        Assert.assertTrue(Arrays.deepEquals(healthcheck.reservoir.get("myQ"), new double[][]{{7.0, 0.0}, {8.0, 0.0}, {9.0, 0.0}}));

        Assert.assertEquals(healthcheck.updateRegression("myQ", 10L, 0L, 3), 0.0, DELTA);
        Assert.assertTrue(Arrays.deepEquals(healthcheck.reservoir.get("myQ"), new double[][]{{8.0, 0.0}, {9.0, 0.0}, {10.0, 0.0}}));
    }
}
