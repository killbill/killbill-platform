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

import java.util.Dictionary;

import org.jruby.Ruby;
import org.killbill.billing.entitlement.plugin.api.EntitlementContext;
import org.killbill.billing.entitlement.plugin.api.EntitlementPluginApi;
import org.killbill.billing.entitlement.plugin.api.EntitlementPluginApiException;
import org.killbill.billing.entitlement.plugin.api.OnFailureEntitlementResult;
import org.killbill.billing.entitlement.plugin.api.OnSuccessEntitlementResult;
import org.killbill.billing.entitlement.plugin.api.PriorEntitlementResult;
import org.killbill.billing.osgi.api.config.PluginRubyConfig;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.payment.api.PluginProperty;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("BC_UNCONFIRMED_CAST")
public class JRubyEntitlementPlugin extends JRubyNotificationPlugin implements EntitlementPluginApi {

    public JRubyEntitlementPlugin(final PluginRubyConfig config, final BundleContext bundleContext, final OSGIConfigPropertiesService configProperties) {
        super(config, bundleContext, configProperties);
    }

    @Override
    protected ServiceRegistration doRegisterService(final BundleContext context, final Dictionary<String, Object> props) {
        return context.registerService(EntitlementPluginApi.class.getName(), this, props);
    }

    @Override
    public PriorEntitlementResult priorCall(final EntitlementContext context, final Iterable<PluginProperty> properties) throws EntitlementPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<PriorEntitlementResult, EntitlementPluginApiException>() {
            @Override
            public PriorEntitlementResult doCall(final Ruby runtime) throws EntitlementPluginApiException {
                return ((EntitlementPluginApi) pluginInstance).priorCall(context, properties);
            }
        });
    }

    @Override
    public OnSuccessEntitlementResult onSuccessCall(final EntitlementContext context, final Iterable<PluginProperty> properties) throws EntitlementPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<OnSuccessEntitlementResult, EntitlementPluginApiException>() {
            @Override
            public OnSuccessEntitlementResult doCall(final Ruby runtime) throws EntitlementPluginApiException {
                return ((EntitlementPluginApi) pluginInstance).onSuccessCall(context, properties);
            }
        });
    }

    @Override
    public OnFailureEntitlementResult onFailureCall(final EntitlementContext context, final Iterable<PluginProperty> properties) throws EntitlementPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<OnFailureEntitlementResult, EntitlementPluginApiException>() {
            @Override
            public OnFailureEntitlementResult doCall(final Ruby runtime) throws EntitlementPluginApiException {
                return ((EntitlementPluginApi) pluginInstance).onFailureCall(context, properties);
            }
        });
    }
}
