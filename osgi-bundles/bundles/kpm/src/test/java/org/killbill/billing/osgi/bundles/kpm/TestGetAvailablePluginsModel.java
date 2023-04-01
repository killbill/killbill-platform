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

package org.killbill.billing.osgi.bundles.kpm;

import org.killbill.billing.osgi.bundles.kpm.PluginManager.GetAvailablePluginsModel;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestGetAvailablePluginsModel {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test(groups = "fast")
    public void testJsonObjectGeneration() throws JsonProcessingException {
        final GetAvailablePluginsModel model = new GetAvailablePluginsModel();
        model.addKillbillVersion("will-not-tested-here");
        model.addPlugins("hello-world", "1.0.0");
        model.addPlugins("stripe", "8.0.0");
        model.addPlugins("braintree", "1.0.0");

        final String result = objectMapper.writeValueAsString(model);
        // Only interested in testing "plugins" object generation
        Assert.assertTrue(result.contains("\"plugins\":{\"braintree\":\"1.0.0\",\"hello-world\":\"1.0.0\",\"stripe\":\"8.0.0\"}"));
    }
}
