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

public interface PluginManager {

    String PROPERTY_PREFIX = "org.killbill.billing.plugin.kpm.";

    GetAvailablePluginsModel getAvailablePlugins(final String kbVersion, final boolean forceDownload) throws KPMPluginException;

    void install(final String uri, final String pluginKey, final String pluginVersion) throws KPMPluginException;

    // KPMWrapper have parameter named 'pluginPackaging'. Get removed since its value will always 'JAR'
    // KPMWrapper have parameter named 'pluginType', because back then, we have 'install_java_plugins' vs
    //   'install_ruby_plugins'. Now we have just 'install_java_plugins'
    void install(final String pluginKey,
                 final String killbillVersion,
                 final String pluginGroupId,
                 final String pluginArtifactId,
                 final String pluginVersion,
                 final String pluginClassifier,
                 final boolean forceDownload) throws KPMPluginException;

    void uninstall(final String pluginKey, final String version) throws KPMPluginException;
}
