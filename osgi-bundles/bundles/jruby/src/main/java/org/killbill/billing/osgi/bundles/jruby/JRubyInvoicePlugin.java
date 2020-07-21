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
import java.util.List;

import org.jruby.Ruby;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.plugin.api.InvoiceContext;
import org.killbill.billing.invoice.plugin.api.InvoicePluginApi;
import org.killbill.billing.invoice.plugin.api.OnFailureInvoiceResult;
import org.killbill.billing.invoice.plugin.api.OnSuccessInvoiceResult;
import org.killbill.billing.invoice.plugin.api.PriorInvoiceResult;
import org.killbill.billing.osgi.api.config.PluginRubyConfig;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.util.callcontext.CallContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("BC_UNCONFIRMED_CAST")
public class JRubyInvoicePlugin extends JRubyNotificationPlugin implements InvoicePluginApi {

    public JRubyInvoicePlugin(final PluginRubyConfig config, final BundleContext bundleContext, final LogService logger, final OSGIConfigPropertiesService configProperties) {
        super(config, bundleContext, logger, configProperties);
    }

    @Override
    protected ServiceRegistration doRegisterService(final BundleContext context, final Dictionary<String, Object> props) {
        return context.registerService(InvoicePluginApi.class.getName(), this, props);
    }

    @Override
    public PriorInvoiceResult priorCall(final InvoiceContext context, final Iterable<PluginProperty> properties) {
        return callWithRuntimeAndChecking(new PluginCallback<PriorInvoiceResult, RuntimeException>() {
            @Override
            public PriorInvoiceResult doCall(final Ruby runtime) {
                return ((InvoicePluginApi) pluginInstance).priorCall(context, properties);
            }
        });
    }

    @Override
    public List<InvoiceItem> getAdditionalInvoiceItems(final Invoice invoice, final boolean dryRun, final Iterable<PluginProperty> properties, final CallContext context) {
        return callWithRuntimeAndChecking(new PluginCallback<List<InvoiceItem>, RuntimeException>() {
            @Override
            public List<InvoiceItem> doCall(final Ruby runtime) {
                return ((InvoicePluginApi) pluginInstance).getAdditionalInvoiceItems(invoice, dryRun, properties, context);
            }
        });
    }

    @Override
    public OnSuccessInvoiceResult onSuccessCall(final InvoiceContext context, final Iterable<PluginProperty> properties) {
        return callWithRuntimeAndChecking(new PluginCallback<OnSuccessInvoiceResult, RuntimeException>() {
            @Override
            public OnSuccessInvoiceResult doCall(final Ruby runtime) {
                return ((InvoicePluginApi) pluginInstance).onSuccessCall(context, properties);
            }
        });
    }

    @Override
    public OnFailureInvoiceResult onFailureCall(final InvoiceContext context, final Iterable<PluginProperty> properties) {
        return callWithRuntimeAndChecking(new PluginCallback<OnFailureInvoiceResult, RuntimeException>() {
            @Override
            public OnFailureInvoiceResult doCall(final Ruby runtime) {
                return ((InvoicePluginApi) pluginInstance).onFailureCall(context, properties);
            }
        });
    }
}
