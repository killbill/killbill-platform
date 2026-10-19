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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.killbill.billing.osgi.bundles.logger.BasicAuthFilter.FailureRateLimiter;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestFailureRateLimiter {

    private static final int MAX_FAILURES = 3;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Test(groups = "fast")
    public void testNotRateLimitedInitially() {
        final FailureRateLimiter limiter = newLimiter(Instant.EPOCH);
        Assert.assertFalse(limiter.isRateLimited("1.2.3.4"));
    }

    @Test(groups = "fast")
    public void testNotRateLimitedUnderThreshold() {
        final MutableClock clock = new MutableClock(Instant.EPOCH);
        final FailureRateLimiter limiter = new FailureRateLimiter(MAX_FAILURES, WINDOW, clock);

        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");

        Assert.assertFalse(limiter.isRateLimited("1.2.3.4"));
    }

    @Test(groups = "fast")
    public void testRateLimitedAtThreshold() {
        final MutableClock clock = new MutableClock(Instant.EPOCH);
        final FailureRateLimiter limiter = new FailureRateLimiter(MAX_FAILURES, WINDOW, clock);

        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");

        Assert.assertTrue(limiter.isRateLimited("1.2.3.4"));
    }

    @Test(groups = "fast")
    public void testDifferentKeysAreIndependent() {
        final MutableClock clock = new MutableClock(Instant.EPOCH);
        final FailureRateLimiter limiter = new FailureRateLimiter(MAX_FAILURES, WINDOW, clock);

        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");

        Assert.assertTrue(limiter.isRateLimited("1.2.3.4"));
        Assert.assertFalse(limiter.isRateLimited("5.6.7.8"));
    }

    @Test(groups = "fast")
    public void testWindowExpiryResetsCount() {
        final MutableClock clock = new MutableClock(Instant.EPOCH);
        final FailureRateLimiter limiter = new FailureRateLimiter(MAX_FAILURES, WINDOW, clock);

        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        Assert.assertTrue(limiter.isRateLimited("1.2.3.4"));

        // Advance past the window: the old failures should no longer count.
        clock.advance(WINDOW.plusSeconds(1));
        Assert.assertFalse(limiter.isRateLimited("1.2.3.4"));

        // A single new failure after expiry should not immediately re-trip the limiter.
        limiter.recordFailure("1.2.3.4");
        Assert.assertFalse(limiter.isRateLimited("1.2.3.4"));
    }

    @Test(groups = "fast")
    public void testSuccessResetsFailureCount() {
        final MutableClock clock = new MutableClock(Instant.EPOCH);
        final FailureRateLimiter limiter = new FailureRateLimiter(MAX_FAILURES, WINDOW, clock);

        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        limiter.recordSuccess("1.2.3.4");

        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");

        // 2 failures after the reset, below MAX_FAILURES (3): should not be rate limited.
        Assert.assertFalse(limiter.isRateLimited("1.2.3.4"));
    }

    @Test(groups = "fast")
    public void testRecordSuccessOnUnknownKeyIsNoOp() {
        final FailureRateLimiter limiter = newLimiter(Instant.EPOCH);
        limiter.recordSuccess("never-seen-before");
        Assert.assertFalse(limiter.isRateLimited("never-seen-before"));
    }

    @Test(groups = "fast")
    public void testCleanupIsThrottledUnderHighCardinalityBurst() {
        // Simulate a burst of failures from > CLEANUP_THRESHOLD (10,000) distinct, still-unexpired keys. cleanup()
        // can't shrink the map in that case (nothing is actually expired yet), so without throttling, every
        // subsequent recordFailure() call would re-trigger a wasted full O(n) scan.
        final MutableClock clock = new MutableClock(Instant.EPOCH);
        final FailureRateLimiter limiter = new FailureRateLimiter(MAX_FAILURES, WINDOW, clock);

        for (int i = 0; i < 10_001; i++) {
            limiter.recordFailure("key-" + i);
        }
        Assert.assertTrue(limiter.size() > 10_000);

        // A few seconds later (window not yet expired, throttle interval not yet elapsed): a cleanup attempt must
        // not be allowed to re-scan the whole map again so soon after the burst's own (no-op) attempt.
        clock.advance(Duration.ofSeconds(5));
        limiter.recordFailure("key-after-burst-1");
        Assert.assertTrue(limiter.size() > 10_000, "cleanup should not have run again before the throttle interval elapsed");

        // Once both the failure window and the throttle interval have elapsed, the next recordFailure() should
        // finally be allowed to sweep out all of the now-expired entries.
        clock.advance(WINDOW);
        limiter.recordFailure("key-after-burst-2");
        Assert.assertTrue(limiter.size() < 10_000, "cleanup should have run once the throttle interval elapsed");
    }

    private static FailureRateLimiter newLimiter(final Instant now) {
        return new FailureRateLimiter(MAX_FAILURES, WINDOW, new MutableClock(now));
    }

    /**
     * A {@link Clock} whose {@code instant()} can be advanced on demand, to deterministically test
     * window-expiry logic without relying on wall-clock time or {@code Thread.sleep}.
     */
    private static final class MutableClock extends Clock {

        private final AtomicReference<Instant> now;

        MutableClock(final Instant now) {
            this.now = new AtomicReference<>(now);
        }

        void advance(final Duration duration) {
            now.updateAndGet(instant -> instant.plus(duration));
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }
}
