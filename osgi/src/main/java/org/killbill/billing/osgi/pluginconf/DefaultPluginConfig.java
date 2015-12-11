/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
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

import java.io.File;
import java.util.Properties;

import org.killbill.billing.osgi.api.config.PluginConfig;
import org.killbill.billing.osgi.api.config.PluginLanguage;
import org.killbill.billing.osgi.api.config.PluginType;

public abstract class DefaultPluginConfig implements PluginConfig, Comparable<PluginConfig> {


    private static final String PROP_PLUGIN_TYPE_NAME = "pluginType";

    private final String pluginName;
    private final PluginType pluginType;
    private final String version;
    private final File pluginVersionRoot;
    private final boolean isSelectedForStart;
    private final boolean isDisabled;

    public DefaultPluginConfig(DefaultPluginConfig input, final boolean isSelectedForStart) {
        this.pluginName = input.getPluginName();
        this.version = input.getVersion();
        this.pluginVersionRoot = input.getPluginVersionRoot();
        this.isSelectedForStart = isSelectedForStart;
        this.pluginType = input.getPluginType();
        this.isDisabled = input.isDisabled();
    }


    public DefaultPluginConfig(final String pluginName, final String version, final Properties props, final File pluginVersionRoot, final boolean isVersionToStartLinkedToMe, final boolean isDisabled) {
        this.pluginName = pluginName;
        this.version = version;
        this.pluginVersionRoot = pluginVersionRoot;
        this.isSelectedForStart = isVersionToStartLinkedToMe;
        this.pluginType = PluginType.valueOf(props.getProperty(PROP_PLUGIN_TYPE_NAME, PluginType.__UNKNOWN__.toString()));
        this.isDisabled = isDisabled;
    }

    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public PluginType getPluginType() {
        return pluginType;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getPluginVersionnedName() {
        return pluginName + "-" + version;
    }

    @Override
    public File getPluginVersionRoot() {
        return pluginVersionRoot;
    }

    @Override
    public boolean isSelectedForStart() {
        return isSelectedForStart;
    }

    @Override
    public boolean isDisabled() {
        return isDisabled;
    }


    @Override
    public abstract PluginLanguage getPluginLanguage();

    protected abstract void validate() throws PluginConfigException;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return compareTo((PluginConfig) o) == 0;
    }

    @Override
    public int hashCode() {
        int result = pluginName != null ? pluginName.hashCode() : 0;
        result = 31 * result + (pluginType != null ? pluginType.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (pluginVersionRoot != null ? pluginVersionRoot.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("DefaultPluginConfig");
        sb.append("{pluginName='").append(pluginName).append('\'');
        sb.append(", pluginType=").append(pluginType);
        sb.append(", version='").append(version).append('\'');
        sb.append(", pluginVersionRoot=").append(pluginVersionRoot);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(final PluginConfig o) {
        if (isSelectedForStart) {
            return -1;
        } else if (o.isSelectedForStart()) {
            return 1;
        } else {
            return -(getVersion().compareTo(o.getVersion()));
        }
    }
}
