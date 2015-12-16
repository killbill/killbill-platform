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

import java.io.IOException;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestPluginIdentifier {

    private ObjectMapper mapper;

    @BeforeMethod(groups = "fast")
    public void beforeMethod() {
        mapper = new ObjectMapper();
    }

    @Test(groups = "fast")
    public void testDeSerialization() throws IOException {

        final String input = "{\"analytics\":{\"plugin_name\":\"analytics-plugin\",\"group_id\":\"org.kill-bill.billing.plugin.java\",\"artifact_id\":\"analytics-plugin\",\"packaging\":\"jar\",\"classifier\":null,\"version\":\"0.7.1\",\"language\":\"java\"},\"payment-test-plugin\":{\"plugin_name\":\"killbill-payment-test\",\"group_id\":\"org.kill-bill.billing.plugin.ruby\",\"artifact_id\":\"payment-test-plugin\",\"packaging\":\"tar.gz\",\"classifier\":null,\"version\":\"1.8.7\",\"language\":\"ruby\"},\"stripe\":{\"plugin_name\":\"killbill-stripe\",\"group_id\":\"org.kill-bill.billing.plugin.ruby\",\"artifact_id\":\"stripe-plugin\",\"packaging\":\"tar.gz\",\"classifier\":null,\"version\":\"2.0.0\",\"language\":\"ruby\"}}";
        final Map<String, PluginIdentifier> output = mapper.readValue(input, new TypeReference<Map<String, PluginIdentifier>>() {});
        Assert.assertEquals(output.size(), 3);

        PluginIdentifier expectedValue = new PluginIdentifier("analytics-plugin", "org.kill-bill.billing.plugin.java", "analytics-plugin", "jar", null, "0.7.1", "java");
        Assert.assertEquals(output.get("analytics"), expectedValue);
    }
}
