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

package org.killbill.billing.osgi.bundles.jruby;

import java.util.Dictionary;
import java.util.Hashtable;

import org.jruby.Ruby;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.api.config.PluginRubyConfig;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.routing.plugin.api.OnFailurePaymentRoutingResult;
import org.killbill.billing.routing.plugin.api.OnSuccessPaymentRoutingResult;
import org.killbill.billing.routing.plugin.api.PaymentRoutingApiException;
import org.killbill.billing.routing.plugin.api.PaymentRoutingContext;
import org.killbill.billing.routing.plugin.api.PaymentRoutingPluginApi;
import org.killbill.billing.routing.plugin.api.PriorPaymentRoutingResult;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

public class JRubyPaymentControlPlugin extends JRubyNotificationPlugin implements PaymentRoutingPluginApi {

    private volatile ServiceRegistration serviceRegistration;

    public JRubyPaymentControlPlugin(final PluginRubyConfig config, final BundleContext bundleContext, final LogService logger, final OSGIConfigPropertiesService configProperties) {
        super(config, bundleContext, logger, configProperties);
    }

    @Override
    public void startPlugin(final BundleContext context) {
        super.startPlugin(context);

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("name", pluginMainClass);
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, pluginGemName);
        serviceRegistration = context.registerService(PaymentRoutingPluginApi.class.getName(), this, props);
    }

    @Override
    public void stopPlugin(final BundleContext context) {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
        super.stopPlugin(context);
    }

    @Override
    public PriorPaymentRoutingResult priorCall(final PaymentRoutingContext context, final Iterable<PluginProperty> properties) throws PaymentRoutingApiException {
        try {
            return callWithRuntimeAndChecking(new PluginCallback<PriorPaymentRoutingResult, PaymentRoutingApiException>() {
                @Override
                public PriorPaymentRoutingResult doCall(final Ruby runtime) throws PaymentRoutingApiException {
                    return ((PaymentRoutingPluginApi) pluginInstance).priorCall(context, properties);
                }
            });
        } catch (PaymentRoutingApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OnSuccessPaymentRoutingResult onSuccessCall(final PaymentRoutingContext context, final Iterable<PluginProperty> properties) throws PaymentRoutingApiException {
        try {
            return callWithRuntimeAndChecking(new PluginCallback<OnSuccessPaymentRoutingResult, PaymentRoutingApiException>() {
                @Override
                public OnSuccessPaymentRoutingResult doCall(final Ruby runtime) throws PaymentRoutingApiException {
                    return ((PaymentRoutingPluginApi) pluginInstance).onSuccessCall(context, properties);
                }
            });
        } catch (PaymentRoutingApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OnFailurePaymentRoutingResult onFailureCall(final PaymentRoutingContext context, final Iterable<PluginProperty> properties) throws PaymentRoutingApiException {
        try {
            return callWithRuntimeAndChecking(new PluginCallback<OnFailurePaymentRoutingResult, PaymentRoutingApiException>() {
                @Override
                public OnFailurePaymentRoutingResult doCall(final Ruby runtime) throws PaymentRoutingApiException {
                    return ((PaymentRoutingPluginApi) pluginInstance).onFailureCall(context, properties);
                }
            });
        } catch (PaymentRoutingApiException e) {
            throw new RuntimeException(e);
        }
    }
}