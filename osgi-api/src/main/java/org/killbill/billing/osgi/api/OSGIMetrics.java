/*
 * Copyright 2015 Groupon, Inc
 *
 * Groupon licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.osgi.api;

public interface OSGIMetrics {
    void markMeter(String meterName);

    void recordHistogramValue(String histogramName, long value);

    void incrementCounter(String counterName);

    void incrementCounter(String counterName, long step);

    void decrementCounter(String counterName);

    void decrementCounter(String counterName, long step);
}
