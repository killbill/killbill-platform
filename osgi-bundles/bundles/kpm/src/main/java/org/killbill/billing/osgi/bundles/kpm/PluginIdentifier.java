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

import java.util.Objects;

import org.killbill.commons.utils.annotation.VisibleForTesting;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class PluginIdentifier {

    private final String pluginName;
    private final String groupId;
    private final String artifactId;
    private final String packaging = "jar";
    private final String classifier;
    private final String version;
    private final String language = "java";

    private PluginIdentifier(final String pluginName,
                             final String groupId,
                             final String artifactId,
                             final String classifier,
                             final String version) {
        this.pluginName = pluginName;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.version = version;
    }

    public PluginIdentifier(final String pluginName, final String version) {
        this(pluginName, null, null, null, version);
    }

    // Required by jackson.
    @VisibleForTesting
    PluginIdentifier() {
        this(null, null, null, null, null);
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getPackaging() {
        return packaging;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getVersion() {
        return version;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PluginIdentifier that = (PluginIdentifier) o;
        return pluginName.equals(that.pluginName) &&
               Objects.equals(groupId, that.groupId) &&
               Objects.equals(artifactId, that.artifactId) &&
               Objects.equals(classifier, that.classifier) &&
               version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginName, groupId, artifactId, packaging, classifier, version, language);
    }
}
