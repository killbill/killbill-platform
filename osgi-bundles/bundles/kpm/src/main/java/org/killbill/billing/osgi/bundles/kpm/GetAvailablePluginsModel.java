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

import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.killbill.billing.osgi.bundles.kpm.AvailablePluginsProvider.AvailablePluginsModel;
import org.killbill.commons.utils.annotation.VisibleForTesting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public final class GetAvailablePluginsModel {

    @JsonProperty("killbill")
    private final KillbillArtifactsVersion killbillArtifactsVersion;

    @JsonProperty("plugins")
    @JsonSerialize(using = AvailablePluginsSerializer.class)
    private final SortedSet<AvailablePluginsModel> availablePlugins;

    public GetAvailablePluginsModel() {
        killbillArtifactsVersion = new KillbillArtifactsVersion();
        availablePlugins = new TreeSet<>();
    }

    public void addKillbillVersion(final String fixedKillbillVersion) {
        killbillArtifactsVersion.setKillbill(fixedKillbillVersion);
    }

    public void addOssParentVersion(final String ossParentVersion) {
        killbillArtifactsVersion.setOssParent(ossParentVersion);
    }

    public void addApiVersion(final String killbillApiVersion) {
        killbillArtifactsVersion.setApi(killbillApiVersion);
    }

    public void addPluginApiVersion(final String killbillPluginApiVersion) {
        killbillArtifactsVersion.setPluginApi(killbillPluginApiVersion);
    }

    public void addCommonsVersion(final String killbillCommonsVersion) {
        killbillArtifactsVersion.setCommons(killbillCommonsVersion);
    }

    public void addPlatformVersion(final String killbillPlatformVersion) {
        killbillArtifactsVersion.setPlatform(killbillPlatformVersion);
    }

    public void addPlugins(final String pluginKey, final String version) {
        availablePlugins.add(new AvailablePluginsModel(pluginKey, version));
    }

    public KillbillArtifactsVersion getKillbillArtifactsVersion() {
        return new KillbillArtifactsVersion(killbillArtifactsVersion);
    }

    @VisibleForTesting
    public SortedSet<AvailablePluginsModel> getAvailablePlugins() {
        return new TreeSet<>(availablePlugins);
    }

    public static class KillbillArtifactsVersion {

        @JsonProperty("killbill")
        private String killbill = "";

        @JsonProperty("killbill-oss-parent")
        private String ossParent = "";

        @JsonProperty("killbill-api")
        private String api = "";

        @JsonProperty("killbill-plugin-api")
        private String pluginApi = "";

        @JsonProperty("killbill-commons")
        private String commons = "";

        @JsonProperty("killbill-platform")
        private String platform = "";

        public KillbillArtifactsVersion() {}

        KillbillArtifactsVersion(final KillbillArtifactsVersion other) {
            this.killbill = other.getKillbill();
            this.ossParent = other.getOssParent();
            this.api = other.getApi();
            this.pluginApi = other.getPluginApi();
            this.commons = other.getCommons();
            this.platform = other.getPlatform();
        }

        public String getKillbill() {
            return killbill;
        }

        public void setKillbill(final String killbill) {
            this.killbill = killbill;
        }

        public String getOssParent() {
            return ossParent;
        }

        public void setOssParent(final String ossParent) {
            this.ossParent = ossParent;
        }

        public String getApi() {
            return api;
        }

        public void setApi(final String api) {
            this.api = api;
        }

        public String getPluginApi() {
            return pluginApi;
        }

        public void setPluginApi(final String pluginApi) {
            this.pluginApi = pluginApi;
        }

        public String getCommons() {
            return commons;
        }

        public void setCommons(final String commons) {
            this.commons = commons;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(final String platform) {
            this.platform = platform;
        }
    }

    static class AvailablePluginsSerializer extends StdSerializer<SortedSet<AvailablePluginsModel>> {

        protected AvailablePluginsSerializer() {
            this(null);
        }

        protected AvailablePluginsSerializer(final Class<SortedSet<AvailablePluginsModel>> t) {
            super(t);
        }

        @Override
        public void serialize(final SortedSet<AvailablePluginsModel> value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            for (final AvailablePluginsModel model : value) {
                gen.writeStringField(model.getPluginKey(), model.getPluginVersion());
            }
            gen.writeEndObject();
        }
    }
}
