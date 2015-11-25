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

package org.killbill.billing.osgi.pluginconf;

import java.io.File;

import org.killbill.billing.osgi.config.OSGIConfig;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestPluginFinder {

    private PluginFinder pluginFinder;

    @BeforeClass(groups = "fast")
    public void beforeClass() {
        /*
        File.createTempFile();
        final OSGIConfig osgiConfig = createOSGIConfig();
        pluginFinder = new PluginFinder();
        */
    }

    @Test(groups = "fast")
    public void testFoo() {

    }


    private OSGIConfig createOSGIConfig(final String rootInstallationDir, final String propertyName) {
        return new OSGIConfig() {
            @Override
            public String getOSGIKillbillPropertyName() {
                return propertyName;
            }
            @Override
            public String getOSGIBundleRootDir() {
                return null;
            }
            @Override
            public String getOSGIBundleCacheName() {
                return null;
            }
            @Override
            public String getRootInstallationDir() {
                return rootInstallationDir;
            }
            @Override
            public String getSystemBundleExportPackagesApi() {
                return null;
            }
            @Override
            public String getSystemBundleExportPackagesJava() {
                return null;
            }
            @Override
            public String getSystemBundleExportPackagesExtra() {
                return null;
            }
        };
    }

}
