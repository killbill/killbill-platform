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

/**
 * Provide various killbill core libraries version, especially for used with {@link PluginManager#getAvailablePlugins(String, boolean)}
 */
public interface VersionsProvider {

    /**
     * Return valid, sem-ver compliance killbill version. No 'LATEST' value, but it is possible that it return
     * "-SNAPSHOT" version.
     */
    String getFixedKillbillVersion();

    String getOssParentVersion();

    String getKillbillApiVersion();

    String getKillbillPluginApiVersion();

    String getKillbillCommonsVersion();

    String getKillbillPlatformVersion();

    VersionsProvider ZERO = new ZeroVersionsProvider();

    static class ZeroVersionsProvider implements VersionsProvider {

        private static final String ZERO_VERSION = "0.0.0";

        @Override
        public String getFixedKillbillVersion() {
            return ZERO_VERSION;
        }

        @Override
        public String getOssParentVersion() {
            return ZERO_VERSION;
        }

        @Override
        public String getKillbillApiVersion() {
            return ZERO_VERSION;
        }

        @Override
        public String getKillbillPluginApiVersion() {
            return ZERO_VERSION;
        }

        @Override
        public String getKillbillCommonsVersion() {
            return ZERO_VERSION;
        }

        @Override
        public String getKillbillPlatformVersion() {
            return ZERO_VERSION;
        }
    }
}
