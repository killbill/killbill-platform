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

package org.killbill.billing.osgi.pluginconf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.killbill.billing.osgi.api.config.PluginConfig;
import org.killbill.commons.utils.io.Files;
import org.testng.Assert;
import org.testng.annotations.Test;

// Verify ordering from PluginFinder#loadPluginsIfRequired
public class TestDefaultPluginConfig {

    @Test(groups = "fast")
    public void testVerifyVersionsOrdering() throws PluginConfigException {
        final PluginConfig javaConfig100 = new DummyJavaConfig("1.0.0", false);
        final PluginConfig javaConfig101 = new DummyJavaConfig("1.0.1", true);
        final PluginConfig javaConfig200 = new DummyJavaConfig("2.0.0", false);
        final PluginConfig javaConfig201 = new DummyJavaConfig("2.0.1", false);

        final List<PluginConfig> configs = new ArrayList<PluginConfig>();
        configs.add(javaConfig100);
        configs.add(javaConfig101);
        configs.add(javaConfig200);
        configs.add(javaConfig201);

        Collections.sort(configs);

        Assert.assertEquals(configs.get(0), javaConfig101);
        Assert.assertEquals(configs.get(1), javaConfig201);
        Assert.assertEquals(configs.get(2), javaConfig200);
        Assert.assertEquals(configs.get(3), javaConfig100);
    }

    private static final class DummyJavaConfig extends DefaultPluginJavaConfig {

        public DummyJavaConfig(final String version, final boolean isSelectedForStart) throws PluginConfigException {
            super("stripe", "killbill-stripe", version, Files.createTempDirectory(), new Properties(), isSelectedForStart, true);
        }

        @Override
        protected void validate() throws PluginConfigException {
        }
    }

}




