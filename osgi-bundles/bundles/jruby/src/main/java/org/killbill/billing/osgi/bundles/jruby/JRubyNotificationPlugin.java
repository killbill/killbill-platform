/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

package org.killbill.billing.osgi.bundles.jruby;

import org.jruby.Ruby;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.notification.plugin.api.NotificationPluginApi;
import org.killbill.billing.osgi.api.config.PluginRubyConfig;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.osgi.framework.BundleContext;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("BC_UNCONFIRMED_CAST")
public class JRubyNotificationPlugin extends JRubyPlugin implements OSGIKillbillEventHandler {

    public JRubyNotificationPlugin(final PluginRubyConfig config, final BundleContext bundleContext, final OSGIConfigPropertiesService configProperties) {
        super(config, bundleContext, configProperties);
    }

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {
        callWithRuntimeAndChecking(new PluginCallback<Void, RuntimeException>() {
            @Override
            public Void doCall(final Ruby runtime) throws RuntimeException {
                ((NotificationPluginApi) pluginInstance).onEvent(killbillEvent);
                return null;
            }
        });
    }
}
