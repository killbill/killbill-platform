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

import java.util.Properties;

import org.killbill.billing.osgi.api.config.PluginConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.io.Files;

// Verify ordering from PluginFinder#loadPluginsIfRequired
public class TestDefaultPluginConfig {

    @Test(groups = "fast")
    public void testVerifyVersionsOrdering() throws PluginConfigException {
        final PluginConfig javaConfig100 = new DummyJavaConfig("1.0.0");
        final PluginConfig javaConfig101 = new DummyJavaConfig("1.0.1");
        final PluginConfig javaConfig200 = new DummyJavaConfig("2.0.0");
        final PluginConfig javaConfig201 = new DummyJavaConfig("2.0.1");
        final PluginConfig javaConfigLATEST = new DummyJavaConfig("LATEST");
        Assert.assertTrue(javaConfig100.getVersion().compareTo(javaConfig101.getVersion()) < 0);
        Assert.assertTrue(javaConfig100.getVersion().compareTo(javaConfig200.getVersion()) < 0);
        Assert.assertTrue(javaConfig100.getVersion().compareTo(javaConfig201.getVersion()) < 0);
        Assert.assertTrue(javaConfig100.getVersion().compareTo(javaConfigLATEST.getVersion()) < 0);
        Assert.assertTrue(javaConfig101.getVersion().compareTo(javaConfig200.getVersion()) < 0);
        Assert.assertTrue(javaConfig101.getVersion().compareTo(javaConfig201.getVersion()) < 0);
        Assert.assertTrue(javaConfig101.getVersion().compareTo(javaConfigLATEST.getVersion()) < 0);
        Assert.assertTrue(javaConfig200.getVersion().compareTo(javaConfig201.getVersion()) < 0);
        Assert.assertTrue(javaConfig200.getVersion().compareTo(javaConfigLATEST.getVersion()) < 0);
        Assert.assertTrue(javaConfig201.getVersion().compareTo(javaConfigLATEST.getVersion()) < 0);
    }

    private static final class DummyJavaConfig extends DefaultPluginJavaConfig {

        public DummyJavaConfig(final String version) throws PluginConfigException {
            super("killbill-stripe", version, Files.createTempDir(), new Properties());
        }

        @Override
        protected void validate() throws PluginConfigException {
        }
    }
}
