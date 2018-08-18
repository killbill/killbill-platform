/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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

package org.killbill.billing.osgi.bundles.rpc;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.notification.plugin.api.NotificationPluginApi;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.GatewayNotification;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.rpc.common.gen.CallContext;
import org.killbill.billing.rpc.common.gen.CallContext.CallOrigin;
import org.killbill.billing.rpc.common.gen.CallContext.UserType;
import org.killbill.billing.rpc.plugin.payment.gen.PaymentPluginApiGrpc;
import org.killbill.billing.rpc.plugin.payment.gen.PaymentRequest;
import org.killbill.billing.rpc.plugin.payment.gen.PaymentRequest.Builder;
import org.killbill.billing.rpc.plugin.payment.gen.PaymentTransactionInfoPlugin;
import org.killbill.billing.rpc.plugin.payment.gen.PluginProperty;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.grpc.ManagedChannel;

public class RPCPaymentPluginApiClient implements PaymentPluginApi, NotificationPluginApi {

    private ManagedChannel channel;

    public RPCPaymentPluginApiClient(final ManagedChannel channel) {
        this.channel = channel;
    }

    @Override
    public void onEvent(final ExtBusEvent killbillEvent) {

    }

    @Override
    public org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin authorizePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final org.killbill.billing.util.callcontext.CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin capturePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final org.killbill.billing.util.callcontext.CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin purchasePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final org.killbill.billing.util.callcontext.CallContext context) throws PaymentPluginApiException {
        final PaymentPluginApiGrpc.PaymentPluginApiBlockingStub stub = PaymentPluginApiGrpc.newBlockingStub(channel);
        final Builder builder = PaymentRequest.newBuilder()
                                              .setKbAccountId(kbAccountId.toString())
                                              .setKbPaymentId(kbPaymentId.toString())
                                              .setKbTransactionId(kbTransactionId.toString())
                                              .setKbPaymentMethodId(kbPaymentMethodId.toString())
                                              .setAmount(amount.toString())
                                              .setCurrency(currency.toString());

        populateProperties(builder, properties);
        final PaymentRequest request = setPaymentRequestContext(context, builder).build();

        final PaymentTransactionInfoPlugin response = stub.purchasePayment(request);
        return new org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin() {
            @Override
            public UUID getKbPaymentId() {
                return UUID.fromString(response.getKbPaymentId());
            }

            @Override
            public UUID getKbTransactionPaymentId() {
                return UUID.fromString(response.getKbTransactionPaymentId());
            }

            @Override
            public TransactionType getTransactionType() {
                return TransactionType.valueOf(response.getTransactionType().toString());
            }

            @Override
            public BigDecimal getAmount() {
                return new BigDecimal(response.getAmount());
            }

            @Override
            public Currency getCurrency() {
                return Currency.valueOf(response.getCurrency());
            }

            @Override
            public DateTime getCreatedDate() {
                return null;
            }

            @Override
            public DateTime getEffectiveDate() {
                return new DateTime(response.getEffectiveDate());
            }

            @Override
            public PaymentPluginStatus getStatus() {
                return PaymentPluginStatus.valueOf(response.getGetStatus().toString());
            }

            @Override
            public String getGatewayError() {
                return response.getGatewayError();
            }

            @Override
            public String getGatewayErrorCode() {
                return response.getGatewayErrorCode();
            }

            @Override
            public String getFirstPaymentReferenceId() {
                return response.getFirstPaymentReferenceId();
            }

            @Override
            public String getSecondPaymentReferenceId() {
                return response.getSecondPaymentReferenceId();
            }

            @Override
            public List<org.killbill.billing.payment.api.PluginProperty> getProperties() {
                return Lists.<PluginProperty, org.killbill.billing.payment.api.PluginProperty>transform(response.getPropertiesList(),
                                                                                                        new Function<PluginProperty, org.killbill.billing.payment.api.PluginProperty>() {
                                                                                                            @Override
                                                                                                            public org.killbill.billing.payment.api.PluginProperty apply(final PluginProperty input) {
                                                                                                                return new org.killbill.billing.payment.api.PluginProperty(input.getKey(),
                                                                                                                                                                           input.getValue(),
                                                                                                                                                                           input.getIsUpdatable());
                                                                                                            }
                                                                                                        });
            }
        };
    }

    @Override
    public org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin voidPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final org.killbill.billing.util.callcontext.CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin creditPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final org.killbill.billing.util.callcontext.CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin refundPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final org.killbill.billing.util.callcontext.CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public List<org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin> getPaymentInfo(final UUID kbAccountId, final UUID kbPaymentId, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        final PaymentPluginApiGrpc.PaymentPluginApiBlockingStub stub = PaymentPluginApiGrpc.newBlockingStub(channel);
        final Builder builder = PaymentRequest.newBuilder()
                                              .setKbAccountId(kbAccountId.toString())
                                              .setKbPaymentId(kbPaymentId.toString());
        populateProperties(builder, properties);
        final CallContext.Builder newBuilder = CallContext.newBuilder().setTenantId(context.getTenantId().toString());
        // TODO KB BUG
        if (context.getAccountId() != null) {
            newBuilder.setAccountId(context.getAccountId().toString());
        }

        final PaymentRequest request = builder.setContext(newBuilder.build()).build();

        final List<org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin> res = new LinkedList<org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin>();
        for (final Iterator<PaymentTransactionInfoPlugin> it = stub.getPaymentInfo(request); it.hasNext(); ) {
            final PaymentTransactionInfoPlugin paymentTransactionInfoPlugin = it.next();
            res.add(new org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin() {
                @Override
                public UUID getKbPaymentId() {
                    return UUID.fromString(paymentTransactionInfoPlugin.getKbPaymentId());
                }

                @Override
                public UUID getKbTransactionPaymentId() {
                    return UUID.fromString(paymentTransactionInfoPlugin.getKbTransactionPaymentId());
                }

                @Override
                public TransactionType getTransactionType() {
                    return TransactionType.valueOf(paymentTransactionInfoPlugin.getTransactionType().name());
                }

                @Override
                public BigDecimal getAmount() {
                    return new BigDecimal(paymentTransactionInfoPlugin.getAmount());
                }

                @Override
                public Currency getCurrency() {
                    return Currency.valueOf(paymentTransactionInfoPlugin.getCurrency());
                }

                @Override
                public DateTime getCreatedDate() {
                    return new DateTime(paymentTransactionInfoPlugin.getCreatedDate());
                }

                @Override
                public DateTime getEffectiveDate() {
                    return new DateTime(paymentTransactionInfoPlugin.getEffectiveDate());
                }

                @Override
                public PaymentPluginStatus getStatus() {
                    return PaymentPluginStatus.valueOf(paymentTransactionInfoPlugin.getGetStatus().name());
                }

                @Override
                public String getGatewayError() {
                    return paymentTransactionInfoPlugin.getGatewayError();
                }

                @Override
                public String getGatewayErrorCode() {
                    return paymentTransactionInfoPlugin.getGatewayErrorCode();
                }

                @Override
                public String getFirstPaymentReferenceId() {
                    return paymentTransactionInfoPlugin.getFirstPaymentReferenceId();
                }

                @Override
                public String getSecondPaymentReferenceId() {
                    return paymentTransactionInfoPlugin.getSecondPaymentReferenceId();
                }

                @Override
                public List<org.killbill.billing.payment.api.PluginProperty> getProperties() {
                    return Lists.<PluginProperty, org.killbill.billing.payment.api.PluginProperty>transform(paymentTransactionInfoPlugin.getPropertiesList(),
                                                                                                            new Function<PluginProperty, org.killbill.billing.payment.api.PluginProperty>() {
                                                                                                                @Override
                                                                                                                public org.killbill.billing.payment.api.PluginProperty apply(final PluginProperty input) {
                                                                                                                    return new org.killbill.billing.payment.api.PluginProperty(input.getKey(),
                                                                                                                                                                               input.getValue(),
                                                                                                                                                                               input.getIsUpdatable());
                                                                                                                }
                                                                                                            });
                }
            });
        }
        return res;
    }

    @Override
    public Pagination<org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin> searchPayments(final String searchKey, final Long offset, final Long limit, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void addPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final PaymentMethodPlugin paymentMethodProps, final boolean setDefault, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final org.killbill.billing.util.callcontext.CallContext context) throws PaymentPluginApiException {
        final PaymentPluginApiGrpc.PaymentPluginApiBlockingStub stub = PaymentPluginApiGrpc.newBlockingStub(channel);
        final Builder builder = PaymentRequest.newBuilder()
                                              .setKbAccountId(kbAccountId.toString())
                                              .setKbPaymentMethodId(kbPaymentMethodId.toString());

        final Iterable<org.killbill.billing.payment.api.PluginProperty> paymentMethodPropsProperties = paymentMethodProps.getProperties();
        populateProperties(builder, paymentMethodPropsProperties);
        populateProperties(builder, properties);
        final PaymentRequest request = setPaymentRequestContext(context, builder).build();
        stub.addPaymentMethod(request);
    }

    private Builder setPaymentRequestContext(final org.killbill.billing.util.callcontext.CallContext context, final Builder builder) {
        final CallContext.Builder newBuilder = CallContext.newBuilder();
        newBuilder
                .setAccountId(context.getAccountId().toString())
                .setTenantId(context.getTenantId().toString())
                .setUserToken(context.getUserToken().toString())
                .setUserName(context.getUserName())
                .setCallOrigin(CallOrigin.valueOf(context.getCallOrigin().toString()))
                .setUserType(UserType.valueOf(context.getUserType().toString()));
        if (context.getReasonCode() != null) {
            newBuilder.setReasonCode(context.getReasonCode());
        }
        if (context.getComments() != null) {
            newBuilder.setComments(context.getComments());
        }
        if (context.getCreatedDate() != null) {
            newBuilder.setCreatedDate(context.getCreatedDate().toString());
        }
        if (context.getUpdatedDate() != null) {
            newBuilder.setUpdatedDate(context.getUpdatedDate().toString());
        }
        return builder.setContext(newBuilder.build());
    }

    private void populateProperties(final Builder builder, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties) {
        for (final org.killbill.billing.payment.api.PluginProperty pluginProperty : properties) {
            final PluginProperty.Builder builderForValue = PluginProperty.newBuilder()
                                                                         .setKey(pluginProperty.getKey())
                                                                         .setValue(pluginProperty.getValue().toString());
            if (pluginProperty.getIsUpdatable() != null) {
                builderForValue.setIsUpdatable(pluginProperty.getIsUpdatable());
            }
            builder.addProperties(builderForValue.build());
        }
    }

    @Override
    public void deletePaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final org.killbill.billing.util.callcontext.CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public PaymentMethodPlugin getPaymentMethodDetail(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void setDefaultPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final org.killbill.billing.util.callcontext.CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(final UUID kbAccountId, final boolean refreshFromGateway, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final org.killbill.billing.util.callcontext.CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public Pagination<PaymentMethodPlugin> searchPaymentMethods(final String searchKey, final Long offset, final Long limit, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void resetPaymentMethods(final UUID kbAccountId, final List<PaymentMethodInfoPlugin> paymentMethods, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final org.killbill.billing.util.callcontext.CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public HostedPaymentPageFormDescriptor buildFormDescriptor(final UUID kbAccountId, final Iterable<org.killbill.billing.payment.api.PluginProperty> customFields, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final org.killbill.billing.util.callcontext.CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public GatewayNotification processNotification(final String notification, final Iterable<org.killbill.billing.payment.api.PluginProperty> properties, final org.killbill.billing.util.callcontext.CallContext context) throws PaymentPluginApiException {
        return null;
    }
}
