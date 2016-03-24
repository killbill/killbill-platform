/*
 * Copyright 2016 Groupon, Inc
 * Copyright 2016 The Billing Project, LLC
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

package org.killbill.billing.osgi;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestJRubyJarHelper {

    final static String POM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                              "<!--\n" +
                              "  ~ Copyright 2010-2013 Ning, Inc.\n" +
                              "  ~ Copyright 2014-2015 Groupon, Inc\n" +
                              "  ~ Copyright 2014-2015 The Billing Project, LLC\n" +
                              "  ~\n" +
                              "  ~ The Billing Project licenses this file to you under the Apache License, version 2.0\n" +
                              "  ~ (the \"License\"); you may not use this file except in compliance with the\n" +
                              "  ~ License.  You may obtain a copy of the License at:\n" +
                              "  ~\n" +
                              "  ~    http://www.apache.org/licenses/LICENSE-2.0\n" +
                              "  ~\n" +
                              "  ~ Unless required by applicable law or agreed to in writing, software\n" +
                              "  ~ distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT\n" +
                              "  ~ WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the\n" +
                              "  ~ License for the specific language governing permissions and limitations\n" +
                              "  ~ under the License.\n" +
                              "  -->\n" +
                              "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                              "    <modelVersion>4.0.0</modelVersion>\n" +
                              "    <parent>\n" +
                              "        <artifactId>killbill-platform-osgi-bundles</artifactId>\n" +
                              "        <groupId>org.kill-bill.billing</groupId>\n" +
                              "        <version>0.22-SNAPSHOT</version>\n" +
                              "        <relativePath>../pom.xml</relativePath>\n" +
                              "    </parent>\n" +
                              "    <artifactId>killbill-platform-osgi-bundles-jruby</artifactId>\n" +
                              "    <packaging>bundle</packaging>\n" +
                              "    <name>killbill-platform-osgi-bundles-jruby</name>\n" +
                              "    bla bla bla\n";

    @Test(groups = "fast")
    public void testExtractVersion() {
        JRubyJarHelper jRubyJarHelper = new JRubyJarHelper(null, null);
        Assert.assertEquals(jRubyJarHelper.extractVersion(POM), "0.22-SNAPSHOT");
    }

}