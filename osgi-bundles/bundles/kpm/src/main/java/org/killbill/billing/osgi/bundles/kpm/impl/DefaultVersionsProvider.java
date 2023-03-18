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

import org.killbill.billing.osgi.bundles.kpm.NexusMetadataFiles;
import org.killbill.billing.osgi.bundles.kpm.VersionsProvider;

public class DefaultVersionsProvider implements VersionsProvider {

    private final String fixedKbVersion;
    private final String ossParentVersion;
    private final String apiVersion;
    private final String pluginApiVersion;
    private final String commonsVersion;
    private final String platformVersion;

    public DefaultVersionsProvider(final NexusMetadataFiles nexusMetadataFiles) throws Exception {
        final XmlParser killbillPomParser = new XmlParser(nexusMetadataFiles.getKillbillPomXml());
        final XmlParser ossParentPomParser = new XmlParser(nexusMetadataFiles.getOssParentPomXml());

        fixedKbVersion = killbillPomParser.getValue("/version");
        ossParentVersion = killbillPomParser.getValue("/parent/version");
        //
        apiVersion = ossParentPomParser.getValue("/properties/killbill-api.version");
        pluginApiVersion = ossParentPomParser.getValue("/properties/killbill-plugin-api.version");
        commonsVersion = ossParentPomParser.getValue("/properties/killbill-commons.version");
        platformVersion = ossParentPomParser.getValue("/properties/killbill-platform.version");
    }

    @Override
    public String getFixedKillbillVersion() {
        return fixedKbVersion;
    }

    @Override
    public String getOssParentVersion() {
        return ossParentVersion;
    }

    @Override
    public String getKillbillApiVersion() {
        return apiVersion;
    }

    @Override
    public String getKillbillPluginApiVersion() {
        return pluginApiVersion;
    }

    @Override
    public String getKillbillCommonsVersion() {
        return commonsVersion;
    }

    @Override
    public String getKillbillPlatformVersion() {
        return platformVersion;
    }

}
