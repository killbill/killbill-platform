/*
 * Copyright 2015 Groupon, Inc
 * Copyright 2015 The Billing Project, LLC
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

import org.killbill.billing.osgi.api.config.PluginConfig;
import org.osgi.framework.Bundle;

public class BundleWithConfig {

    private final Bundle bundle;
    private final PluginConfig config;

    public BundleWithConfig(final Bundle bundle, final PluginConfig config) {
        this.bundle = bundle;
        this.config = config;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public PluginConfig getConfig() {
        return config;
    }
}
