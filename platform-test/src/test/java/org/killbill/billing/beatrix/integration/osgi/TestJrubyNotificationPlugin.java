/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
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

package org.killbill.billing.beatrix.integration.osgi;

import org.killbill.billing.beatrix.integration.osgi.util.SetupBundleWithAssertion;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestJrubyNotificationPlugin extends TestOSGIBase {

    private static final String BUNDLE_TEST_RESOURCE_PREFIX = "killbill-notification-test";
    private static final String BUNDLE_TEST_RESOURCE = BUNDLE_TEST_RESOURCE_PREFIX + ".tar.gz";

    @BeforeClass(groups = "slow")
    public void beforeClass() throws Exception {
        super.beforeClass();

        // This is extracted from surefire system configuration -- needs to be added explicitly in IntelliJ for correct running
        final String killbillVersion = System.getProperty("killbill.version");

        final SetupBundleWithAssertion setupTest = new SetupBundleWithAssertion(BUNDLE_TEST_RESOURCE, osgiConfig, killbillVersion);
        setupTest.setupJrubyBundle();
    }

    @Test(groups = "slow")
    public void testOnEventForAccountCreation() throws Exception {
        // TODO change the ruby plugin not to interact with tags - use events instead?
    }
}
