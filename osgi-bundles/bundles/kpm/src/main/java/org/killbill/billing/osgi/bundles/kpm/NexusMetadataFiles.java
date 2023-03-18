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

import java.nio.file.Path;

/**
 * Get (and download, if necessary) nexus metadata files, like pom.xml, maven-metadata.xml, etc.
 */
public interface NexusMetadataFiles {

    /**
     * Get published <a href="https://github.com/killbill/killbill/blob/master/pom.xml">killbill</a> pom.xml in implemented
     * nexus repository.
     */
    Path getKillbillPomXml() throws Exception;

    /**
     * Get published <a href="https://github.com/killbill/killbill-oss-parent/blob/master/pom.xml">killbill-oss-parent</a>
     * pom.xml in nexus repository.
     */
    Path getOssParentPomXml() throws Exception;
}
