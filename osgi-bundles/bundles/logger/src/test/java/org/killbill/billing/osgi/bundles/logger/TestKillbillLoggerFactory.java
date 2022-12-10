/*
 * Copyright 2020-2022 Equinix, Inc
 * Copyright 2014-2022 The Billing Project, LLC
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

import org.killbill.billing.osgi.bundles.logger.KillbillLoggerFactory.LoggersKey;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestKillbillLoggerFactory {

    Bundle mockBundle(final String bundleName, final String versionString) {
        final Version version = Mockito.mock(Version.class);
        Mockito.when(version.toString()).thenReturn(versionString);

        final Bundle result = Mockito.mock(Bundle.class);
        Mockito.when(result.getSymbolicName()).thenReturn(bundleName);
        Mockito.when(result.getVersion()).thenReturn(version);

        return result;
    }

    @DataProvider(name = "createBundleNameParams")
    Object[][] createBundleNameParams() {
        return new Object[][] {
                { mockBundle("bundle-a", "1.2.3"), "bundle-a:1_2_3" },
                { mockBundle("bundle-b", "1.2"), "bundle-b:1_2" },
                { mockBundle("bundle-c", ""), "bundle-c:0_0_0" },
                { mockBundle("bundle-d", null), "bundle-d:0_0_0" },
                { mockBundle("bundle-d", null), "bundle-d:0_0_0" },
                { mockBundle(null, "1.2.3"), "" },
                { mockBundle("", "1.2.3"), "" },
                { mockBundle("   ", "1.2.3"), "" },
                { null, "" }

        };
    }

    @Test(groups = "fast", dataProvider = "createBundleNameParams")
    public void testLoggerKeysCreateBundleName(final Bundle bundle, final String expectedResult) {
        final String result = LoggersKey.createBundleName(bundle);
        Assert.assertEquals(result, expectedResult);
    }

    @DataProvider(name = "createLoggerNameParams")
    Object[][] createLoggerNameParams() {
        return new Object[][] {
                // Class name take precedence
                { mockBundle("bundle-a", "1.2.3"), "com.foo.Hello", "com.foo.Hello" },
                { null, "com.other.World", "com.other.World" },

                // className null, empty, or blank.
                { mockBundle("bundle-b", "1.2.3"), " ", "bundle-b:1_2_3" },
                { mockBundle("bundle-c", "1.2.3"), "", "bundle-c:1_2_3" },
                { mockBundle("bundle-d", "1.2"), null, "bundle-d:1_2" },
                { mockBundle("bundle-e", null), null, "bundle-e:0_0_0" },

                // bundleName and className not sufficient, fall back to KillbillLogWriter
                { mockBundle(null, "1.2.3"), "", "org.killbill.billing.osgi.bundles.logger.KillbillLogWriter" },
                { null, " ", "org.killbill.billing.osgi.bundles.logger.KillbillLogWriter" },
                { null, null, "org.killbill.billing.osgi.bundles.logger.KillbillLogWriter" }
        };
    }

    @Test(groups = "fast", dataProvider = "createLoggerNameParams")
    public void testLoggerKeysCreateLoggerName(final Bundle bundle, final String className, final String expectedResult) {
        final String result = LoggersKey.createLoggerName(bundle, className);
        Assert.assertEquals(result, expectedResult);
    }

    @Test(groups = "fast")
    public void testGetLogger() {
        final Bundle bundle = mockBundle("super-bundle", "1.0.0");
        final KillbillLoggerFactory loggerFactory = new KillbillLoggerFactory(bundle);

        loggerFactory.getLogger().info("anything..");
        Assert.assertEquals(loggerFactory.getLoggersSize(), 1);

        loggerFactory.getLogger(bundle, null, KillbillLogger.class).info("anything..");
        Assert.assertEquals(loggerFactory.getLoggersSize(), 1);

        loggerFactory.getLogger(String.class).info("anything..");
        loggerFactory.getLogger(Character.class).info("anything..");
        Assert.assertEquals(loggerFactory.getLoggersSize(), 3);

        loggerFactory.getLogger(bundle, Integer.class.getName(), KillbillLogger.class).info("same bundle, different class.");
        Assert.assertEquals(loggerFactory.getLoggersSize(), 4);
    }
}
