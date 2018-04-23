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

package org.killbill.billing.osgi.libs.killbill;

import org.killbill.billing.account.api.AccountUserApi;
import org.killbill.billing.catalog.api.CatalogUserApi;
import org.killbill.billing.currency.api.CurrencyConversionApi;
import org.killbill.billing.entitlement.api.EntitlementApi;
import org.killbill.billing.entitlement.api.SubscriptionApi;
import org.killbill.billing.invoice.api.InvoicePaymentApi;
import org.killbill.billing.invoice.api.InvoiceUserApi;
import org.killbill.billing.osgi.api.PluginsInfoApi;
import org.killbill.billing.osgi.api.ROOSGIKillbillInterceptor;
import org.killbill.billing.osgi.api.config.PluginConfigServiceApi;
import org.killbill.billing.overdue.api.OverdueApi;
import org.killbill.billing.payment.api.AdminPaymentApi;
import org.killbill.billing.payment.api.PaymentApi;
import org.killbill.billing.security.api.SecurityApi;
import org.killbill.billing.tenant.api.TenantUserApi;
import org.killbill.billing.usage.api.UsageUserApi;
import org.killbill.billing.util.api.AuditUserApi;
import org.killbill.billing.util.api.CustomFieldUserApi;
import org.killbill.billing.util.api.ExportUserApi;
import org.killbill.billing.util.api.RecordIdApi;
import org.killbill.billing.util.api.TagUserApi;
import org.killbill.billing.util.nodes.KillbillNodesApi;
import org.osgi.framework.BundleContext;

public class ROOSGIKillbillAPI extends OSGIKillbillAPI {

    public ROOSGIKillbillAPI(final BundleContext context) {
        super(context);
    }

    @Override
    public AccountUserApi getAccountUserApi() {
        return ROOSGIKillbillInterceptor.<AccountUserApi>getProxy(super.getAccountUserApi(), AccountUserApi.class);
    }

    @Override
    public CatalogUserApi getCatalogUserApi() {
        return ROOSGIKillbillInterceptor.<CatalogUserApi>getProxy(super.getCatalogUserApi(), CatalogUserApi.class);
    }

    @Override
    public SubscriptionApi getSubscriptionApi() {
        return ROOSGIKillbillInterceptor.<SubscriptionApi>getProxy(super.getSubscriptionApi(), SubscriptionApi.class);
    }

    @Override
    public InvoicePaymentApi getInvoicePaymentApi() {
        return ROOSGIKillbillInterceptor.<InvoicePaymentApi>getProxy(super.getInvoicePaymentApi(), InvoicePaymentApi.class);
    }

    @Override
    public InvoiceUserApi getInvoiceUserApi() {
        return ROOSGIKillbillInterceptor.<InvoiceUserApi>getProxy(super.getInvoiceUserApi(), InvoiceUserApi.class);
    }

    @Override
    public PaymentApi getPaymentApi() {
        return ROOSGIKillbillInterceptor.<PaymentApi>getProxy(super.getPaymentApi(), PaymentApi.class);
    }

    @Override
    public TenantUserApi getTenantUserApi() {
        return ROOSGIKillbillInterceptor.<TenantUserApi>getProxy(super.getTenantUserApi(), TenantUserApi.class);
    }

    @Override
    public UsageUserApi getUsageUserApi() {
        return ROOSGIKillbillInterceptor.<UsageUserApi>getProxy(super.getUsageUserApi(), UsageUserApi.class);
    }

    @Override
    public AuditUserApi getAuditUserApi() {
        return ROOSGIKillbillInterceptor.<AuditUserApi>getProxy(super.getAuditUserApi(), AuditUserApi.class);
    }

    @Override
    public CustomFieldUserApi getCustomFieldUserApi() {
        return ROOSGIKillbillInterceptor.<CustomFieldUserApi>getProxy(super.getCustomFieldUserApi(), CustomFieldUserApi.class);
    }

    @Override
    public ExportUserApi getExportUserApi() {
        return ROOSGIKillbillInterceptor.<ExportUserApi>getProxy(super.getExportUserApi(), ExportUserApi.class);
    }

    @Override
    public TagUserApi getTagUserApi() {
        return ROOSGIKillbillInterceptor.<TagUserApi>getProxy(super.getTagUserApi(), TagUserApi.class);
    }

    @Override
    public EntitlementApi getEntitlementApi() {
        return ROOSGIKillbillInterceptor.<EntitlementApi>getProxy(super.getEntitlementApi(), EntitlementApi.class);
    }

    @Override
    public RecordIdApi getRecordIdApi() {
        return ROOSGIKillbillInterceptor.<RecordIdApi>getProxy(super.getRecordIdApi(), RecordIdApi.class);
    }

    @Override
    public CurrencyConversionApi getCurrencyConversionApi() {
        return ROOSGIKillbillInterceptor.<CurrencyConversionApi>getProxy(super.getCurrencyConversionApi(), CurrencyConversionApi.class);
    }

    @Override
    public OverdueApi getOverdueApi() {
        return ROOSGIKillbillInterceptor.<OverdueApi>getProxy(super.getOverdueApi(), OverdueApi.class);
    }

    @Override
    public PluginConfigServiceApi getPluginConfigServiceApi() {
        return ROOSGIKillbillInterceptor.<PluginConfigServiceApi>getProxy(super.getPluginConfigServiceApi(), PluginConfigServiceApi.class);
    }

    @Override
    public SecurityApi getSecurityApi() {
        return ROOSGIKillbillInterceptor.<SecurityApi>getProxy(super.getSecurityApi(), SecurityApi.class);
    }

    @Override
    public PluginsInfoApi getPluginsInfoApi() {
        return ROOSGIKillbillInterceptor.<PluginsInfoApi>getProxy(super.getPluginsInfoApi(), PluginsInfoApi.class);
    }

    @Override
    public KillbillNodesApi getKillbillNodesApi() {
        return ROOSGIKillbillInterceptor.<KillbillNodesApi>getProxy(super.getKillbillNodesApi(), KillbillNodesApi.class);
    }

    @Override
    public AdminPaymentApi getAdminPaymentApi() {
        return ROOSGIKillbillInterceptor.<AdminPaymentApi>getProxy(super.getAdminPaymentApi(), AdminPaymentApi.class);
    }
}
