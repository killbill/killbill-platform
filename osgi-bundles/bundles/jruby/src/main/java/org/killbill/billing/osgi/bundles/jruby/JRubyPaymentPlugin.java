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

package org.killbill.billing.osgi.bundles.jruby;

import java.math.BigDecimal;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import org.jruby.Ruby;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.api.config.PluginRubyConfig;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.plugin.api.GatewayNotification;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

public class JRubyPaymentPlugin extends JRubyNotificationPlugin implements PaymentPluginApi {

    private volatile ServiceRegistration serviceRegistration;

    public JRubyPaymentPlugin(final PluginRubyConfig config, final BundleContext bundleContext, final LogService logger, final OSGIConfigPropertiesService configProperties) {
        super(config, bundleContext, logger, configProperties);
    }

    @Override
    public void startPlugin(final BundleContext context) {
        super.startPlugin(context);

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("name", pluginMainClass);
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, pluginGemName);
        serviceRegistration = context.registerService(PaymentPluginApi.class.getName(), this, props);
    }

    @Override
    public void stopPlugin(final BundleContext context) {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
        super.stopPlugin(context);
    }

    @Override
    public PaymentTransactionInfoPlugin authorizePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<PaymentTransactionInfoPlugin, PaymentPluginApiException>() {
            @Override
            public PaymentTransactionInfoPlugin doCall(final Ruby runtime) throws PaymentPluginApiException {
                return ((PaymentPluginApi) pluginInstance).authorizePayment(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
            }
        });
    }

    @Override
    public PaymentTransactionInfoPlugin capturePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<PaymentTransactionInfoPlugin, PaymentPluginApiException>() {
            @Override
            public PaymentTransactionInfoPlugin doCall(final Ruby runtime) throws PaymentPluginApiException {
                return ((PaymentPluginApi) pluginInstance).capturePayment(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
            }
        });
    }

    @Override
    public PaymentTransactionInfoPlugin purchasePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<PaymentTransactionInfoPlugin, PaymentPluginApiException>() {
            @Override
            public PaymentTransactionInfoPlugin doCall(final Ruby runtime) throws PaymentPluginApiException {
                return ((PaymentPluginApi) pluginInstance).purchasePayment(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
            }
        });
    }

    @Override
    public PaymentTransactionInfoPlugin voidPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<PaymentTransactionInfoPlugin, PaymentPluginApiException>() {
            @Override
            public PaymentTransactionInfoPlugin doCall(final Ruby runtime) throws PaymentPluginApiException {
                return ((PaymentPluginApi) pluginInstance).voidPayment(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, properties, context);
            }
        });
    }

    @Override
    public PaymentTransactionInfoPlugin creditPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<PaymentTransactionInfoPlugin, PaymentPluginApiException>() {
            @Override
            public PaymentTransactionInfoPlugin doCall(final Ruby runtime) throws PaymentPluginApiException {
                return ((PaymentPluginApi) pluginInstance).creditPayment(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
            }
        });
    }

    @Override
    public List<PaymentTransactionInfoPlugin> getPaymentInfo(final UUID kbAccountId, final UUID kbPaymentId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<List<PaymentTransactionInfoPlugin>, PaymentPluginApiException>() {
            @Override
            public List<PaymentTransactionInfoPlugin> doCall(final Ruby runtime) throws PaymentPluginApiException {
                return ((PaymentPluginApi) pluginInstance).getPaymentInfo(kbAccountId, kbPaymentId, properties, context);
            }
        });
    }

    @Override
    public Pagination<PaymentTransactionInfoPlugin> searchPayments(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext tenantContext) throws PaymentPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<Pagination<PaymentTransactionInfoPlugin>, PaymentPluginApiException>() {
            @Override
            public Pagination<PaymentTransactionInfoPlugin> doCall(final Ruby runtime) throws PaymentPluginApiException {
                return ((PaymentPluginApi) pluginInstance).searchPayments(searchKey, offset, limit, properties, tenantContext);
            }
        });
    }

    @Override
    public PaymentTransactionInfoPlugin refundPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal refundAmount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<PaymentTransactionInfoPlugin, PaymentPluginApiException>() {
            @Override
            public PaymentTransactionInfoPlugin doCall(final Ruby runtime) throws PaymentPluginApiException {
                return ((PaymentPluginApi) pluginInstance).refundPayment(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, refundAmount, currency, properties, context);
            }
        });

    }

    @Override
    public void addPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final PaymentMethodPlugin paymentMethodProps, final boolean setDefault, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        callWithRuntimeAndChecking(new PluginCallback<Void, PaymentPluginApiException>() {
            @Override
            public Void doCall(final Ruby runtime) throws PaymentPluginApiException {
                ((PaymentPluginApi) pluginInstance).addPaymentMethod(kbAccountId, kbPaymentMethodId, paymentMethodProps, Boolean.valueOf(setDefault), properties, context);
                return null;
            }
        });
    }

    @Override
    public void deletePaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        callWithRuntimeAndChecking(new PluginCallback<Void, PaymentPluginApiException>() {
            @Override
            public Void doCall(final Ruby runtime) throws PaymentPluginApiException {
                ((PaymentPluginApi) pluginInstance).deletePaymentMethod(kbAccountId, kbPaymentMethodId, properties, context);
                return null;
            }
        });
    }

    @Override
    public PaymentMethodPlugin getPaymentMethodDetail(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<PaymentMethodPlugin, PaymentPluginApiException>() {
            @Override
            public PaymentMethodPlugin doCall(final Ruby runtime) throws PaymentPluginApiException {
                return ((PaymentPluginApi) pluginInstance).getPaymentMethodDetail(kbAccountId, kbPaymentMethodId, properties, context);
            }
        });
    }

    @Override
    public void setDefaultPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        callWithRuntimeAndChecking(new PluginCallback<Void, PaymentPluginApiException>() {
            @Override
            public Void doCall(final Ruby runtime) throws PaymentPluginApiException {
                ((PaymentPluginApi) pluginInstance).setDefaultPaymentMethod(kbAccountId, kbPaymentMethodId, properties, context);
                return null;
            }
        });
    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(final UUID kbAccountId, final boolean refreshFromGateway, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<List<PaymentMethodInfoPlugin>, PaymentPluginApiException>() {
            @Override
            public List<PaymentMethodInfoPlugin> doCall(final Ruby runtime) throws PaymentPluginApiException {
                return ((PaymentPluginApi) pluginInstance).getPaymentMethods(kbAccountId, Boolean.valueOf(refreshFromGateway), properties, context);
            }
        });
    }

    @Override
    public Pagination<PaymentMethodPlugin> searchPaymentMethods(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext tenantContext) throws PaymentPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<Pagination<PaymentMethodPlugin>, PaymentPluginApiException>() {
            @Override
            public Pagination<PaymentMethodPlugin> doCall(final Ruby runtime) throws PaymentPluginApiException {
                return ((PaymentPluginApi) pluginInstance).searchPaymentMethods(searchKey, offset, limit, properties, tenantContext);
            }
        });
    }

    @Override
    public void resetPaymentMethods(final UUID kbAccountId, final List<PaymentMethodInfoPlugin> paymentMethods, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        callWithRuntimeAndChecking(new PluginCallback<Void, PaymentPluginApiException>() {
            @Override
            public Void doCall(final Ruby runtime) throws PaymentPluginApiException {
                ((PaymentPluginApi) pluginInstance).resetPaymentMethods(kbAccountId, paymentMethods, properties, context);
                return null;
            }
        });
    }

    @Override
    public HostedPaymentPageFormDescriptor buildFormDescriptor(final UUID kbAccountId, final Iterable<PluginProperty> customFields, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<HostedPaymentPageFormDescriptor, PaymentPluginApiException>() {
            @Override
            public HostedPaymentPageFormDescriptor doCall(final Ruby runtime) throws PaymentPluginApiException {
                return ((PaymentPluginApi) pluginInstance).buildFormDescriptor(kbAccountId, customFields, properties, context);
            }
        });
    }

    @Override
    public GatewayNotification processNotification(final String notification, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return callWithRuntimeAndChecking(new PluginCallback<GatewayNotification, PaymentPluginApiException>() {
            @Override
            public GatewayNotification doCall(final Ruby runtime) throws PaymentPluginApiException {
                return ((PaymentPluginApi) pluginInstance).processNotification(notification, properties, context);
            }
        });
    }
}
