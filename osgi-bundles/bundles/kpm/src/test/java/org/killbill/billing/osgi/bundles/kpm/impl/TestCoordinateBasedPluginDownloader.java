/*
 * Copyright 2020-2023 Equinix, Inc
 * Copyright 2014-2023 The Billing Project, LLC
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

package org.killbill.billing.osgi.bundles.kpm.impl;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestCoordinateBasedPluginDownloader {

    @DataProvider(name = "testCoordinateToUriParams")
    Object[][] testCoordinateToUriParams() {
        return new Object[][] {
                {
                        "org.kill-bill.billing", "killbill-profiles-killbill", "0.24.1",
                        "org/kill-bill/billing/killbill-profiles-killbill/0.24.1/killbill-profiles-killbill-0.24.1.jar"
                },
                {
                        "org.kill-bill.billing", "killbill-platform-osgi-bundles-kpm", "0.41.1",
                        "org/kill-bill/billing/killbill-platform-osgi-bundles-kpm/0.41.1/killbill-platform-osgi-bundles-kpm-0.41.1.jar"
                }
        };
    }

    @Test(groups = "fast", dataProvider = "testCoordinateToUriParams")
    public void testCoordinateToUri(final String groupId, final String artifactId, final String version, final String expected) {
        final String result = CoordinateBasedPluginDownloader.coordinateToUri(groupId, artifactId, version, ".jar");
        Assert.assertEquals(result, expected);
    }
}
