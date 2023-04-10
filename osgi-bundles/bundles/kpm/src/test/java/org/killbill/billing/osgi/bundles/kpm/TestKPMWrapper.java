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

package org.killbill.billing.osgi.bundles.kpm;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Properties;

import org.killbill.billing.osgi.api.PluginsInfoApi;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.plugin.util.http.InvalidRequest;
import org.killbill.billing.security.api.SecurityApi;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.killbill.billing.osgi.bundles.kpm.KPMWrapper.PROPERTY_PREFIX;

@Deprecated
public class TestKPMWrapper {

    private KPMWrapper kpmWrapper;

    @BeforeMethod(groups = "slow", enabled = false)
    public void setUp() throws GeneralSecurityException {
        final OSGIKillbillAPI killbillApi = Mockito.mock(OSGIKillbillAPI.class);
        final SecurityApi securityApi = Mockito.mock(SecurityApi.class);
        Mockito.when(killbillApi.getSecurityApi()).thenReturn(securityApi);
        final PluginsInfoApi pluginsInfoApi = Mockito.mock(PluginsInfoApi.class);
        Mockito.when(killbillApi.getPluginsInfoApi()).thenReturn(pluginsInfoApi);

        final Properties properties = new Properties();
        properties.setProperty(PROPERTY_PREFIX + "kpmPath", "echo");

        kpmWrapper = new KPMWrapper(killbillApi, properties);
    }

    @Test(groups = "slow", enabled = false)
    public void testInstall() throws InvalidRequest, IOException, URISyntaxException, InterruptedException {
        kpmWrapper.install("analytics",
                           "https://repo1.maven.org/maven2/org/kill-bill/billing/plugin/java/analytics-plugin/7.2.6/analytics-plugin-7.2.6.jar",
                           "7.2.6",
                           "java");
    }
}