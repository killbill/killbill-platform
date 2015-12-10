/*
 * Copyright 2015 Groupon, Inc
 * Copyright 2015 The Billing Project, LLC
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

package org.killbill.killbill.osgi.libs.killbill;

import org.killbill.clock.Clock;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class OSGIKillbillClock extends OSGIKillbillLibraryBase {

    private static final String CLOCK_SERVICE_NAME = "org.killbill.clock.Clock";

    private final ServiceTracker clockTracker;

    public OSGIKillbillClock(final BundleContext context) {
        clockTracker = new ServiceTracker(context, CLOCK_SERVICE_NAME, null);
        clockTracker.open();
    }

    public void close() {
        if (clockTracker != null) {
            clockTracker.close();
        }
    }

    public Clock getClock() {
        return withServiceTracker(clockTracker, new APICallback<Clock, Clock>(CLOCK_SERVICE_NAME) {
            @Override
            public Clock executeWithService(final Clock service) {
                return (Clock) clockTracker.getService();
            }
        });
    }
}
