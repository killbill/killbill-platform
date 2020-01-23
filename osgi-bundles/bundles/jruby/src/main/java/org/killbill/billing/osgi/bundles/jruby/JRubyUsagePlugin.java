/*
 * Copyright 2014-2019 Groupon, Inc
 * Copyright 2014-2019 The Billing Project, LLC
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

import java.util.Dictionary;
import java.util.List;
import java.util.UUID;

import org.joda.time.LocalDate;
import org.jruby.Ruby;
import org.killbill.billing.osgi.api.config.PluginRubyConfig;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.usage.api.RawUsageRecord;
import org.killbill.billing.usage.plugin.api.UsagePluginApi;
import org.killbill.billing.util.callcontext.TenantContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

public class JRubyUsagePlugin extends JRubyNotificationPlugin implements UsagePluginApi {

    public JRubyUsagePlugin(final PluginRubyConfig config, final BundleContext bundleContext, final LogService logger, final OSGIConfigPropertiesService configProperties) {
        super(config, bundleContext, logger, configProperties);
    }

    @Override
    protected ServiceRegistration doRegisterService(final BundleContext context, final Dictionary<String, Object> props) {
        return context.registerService(UsagePluginApi.class.getName(), this, props);
    }

    @Override
    public List<RawUsageRecord> getUsageForAccount(final LocalDate startDate, final LocalDate endDate, final TenantContext tenantContext) {
        return callWithRuntimeAndChecking(new PluginCallback<List<RawUsageRecord>, RuntimeException>() {
            @Override
            public List<RawUsageRecord> doCall(final Ruby runtime) {
                return ((UsagePluginApi) pluginInstance).getUsageForAccount(startDate, endDate, tenantContext);
            }
        });
    }

    @Override
    public List<RawUsageRecord> getUsageForSubscription(final UUID subscriptionId, final LocalDate startDate, final LocalDate endDate, final TenantContext tenantContext) {
        return callWithRuntimeAndChecking(new PluginCallback<List<RawUsageRecord>, RuntimeException>() {
            @Override
            public List<RawUsageRecord> doCall(final Ruby runtime) {
                return ((UsagePluginApi) pluginInstance).getUsageForSubscription(subscriptionId, startDate, endDate, tenantContext);
            }
        });
    }
}
