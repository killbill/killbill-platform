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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.killbill.commons.utils.Preconditions;
import org.killbill.commons.utils.Strings;

class PluginNamingResolver {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+[1-9]*).(\\d+).(\\d+)(?:-([a-zA-Z0-9]+))?");

    private final String pluginKey;
    private final String pluginVersion;

    private String stringContainsVersion;

    PluginNamingResolver(final String pluginKey, final String pluginVersion) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(pluginKey), "pluginKey is null or empty");
        this.pluginKey = pluginKey;
        this.pluginVersion = pluginVersion;
    }

    static PluginNamingResolver of(final String pluginKey) {
        return new PluginNamingResolver(pluginKey, null);
    }

    static PluginNamingResolver of(final String pluginKey, final String pluginVersion) {
        return new PluginNamingResolver(pluginKey, pluginVersion);
    }

    /**
     * Create instance of {@code PluginNamingResolver}, guess version value from {@code stringContainsVersion} if
     * {@code pluginVersion} not match any version format.
     */
    static PluginNamingResolver of(final String pluginKey, final String pluginVersion, final String stringContainsVersion) {
        final PluginNamingResolver result = of(pluginKey, pluginVersion);
        result.stringContainsVersion = stringContainsVersion;
        return result;
    }

    String getPluginName() {
        return pluginKey + "-plugin";
    }

    String getPluginVersion() {
        Preconditions.checkState(pluginVersion != null, "Cannot call #getPluginVersion() since PluginNamingResolver created without 'version'");
        String result = getVersionFromString(pluginVersion);
        if (Strings.isNullOrEmpty(result) && !Strings.isNullOrEmpty(stringContainsVersion)) {
            result = getVersionFromString(stringContainsVersion);
        }

        return Strings.isNullOrEmpty(result) ? "0.0.0" : result;
    }

    String getPluginJarFileName() {
        return getPluginName().concat("-").concat(getPluginVersion()).concat(".jar");
    }

    /**
     * <a href="https://github.com/killbill/killbill-cloud/blob/master/kpm/lib/kpm/base_installer.rb#L166">See here</a>.
     */
    static String getVersionFromString(final String string) {
        final Matcher matcher = VERSION_PATTERN.matcher(string);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(0).replaceAll("(?i)-{1,2}SNAPSHOT", "");
    }
}
