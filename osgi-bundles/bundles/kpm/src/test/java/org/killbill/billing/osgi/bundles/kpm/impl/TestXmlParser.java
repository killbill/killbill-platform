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

import java.io.IOException;
import java.nio.file.Path;

import org.killbill.billing.osgi.bundles.kpm.TestUtils;
import org.killbill.billing.osgi.bundles.kpm.impl.XmlParser;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestXmlParser {

    private final Path xmlFile = TestUtils.getTestPath("xml").resolve("killbill-pom.xml");

    @Test(groups = "fast")
    public void testGetValue() throws IOException {
        final XmlParser xmlParser = new XmlParser(xmlFile);
        Assert.assertEquals(xmlParser.getValue("/modelVersion"), "4.0.0");
        Assert.assertEquals(xmlParser.getValue("/version"), "0.24.1-SNAPSHOT");
        Assert.assertEquals(xmlParser.getValue("/not-existent"), "");
        Assert.assertEquals(xmlParser.getValue("/non-maven-element"), "");
    }
}
