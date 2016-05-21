/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
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

import org.killbill.billing.account.api.AccountUserApi;
import org.killbill.billing.catalog.api.CatalogUserApi;
import org.killbill.billing.currency.api.CurrencyConversionApi;
import org.killbill.billing.entitlement.api.EntitlementApi;
import org.killbill.billing.entitlement.api.SubscriptionApi;
import org.killbill.billing.invoice.api.InvoicePaymentApi;
import org.killbill.billing.invoice.api.InvoiceUserApi;
import org.killbill.billing.osgi.api.OSGIKillbill;
import org.killbill.billing.osgi.api.PluginsInfoApi;
import org.killbill.billing.osgi.api.config.PluginConfigServiceApi;
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

import com.google.inject.Inject;

public class DefaultOSGIKillbill implements OSGIKillbill {

    private AccountUserApi accountUserApi;
    private CatalogUserApi catalogUserApi;
    private InvoicePaymentApi invoicePaymentApi;
    private InvoiceUserApi invoiceUserApi;
    private PaymentApi paymentApi;
    private TenantUserApi tenantUserApi;
    private UsageUserApi usageUserApi;
    private AuditUserApi auditUserApi;
    private CustomFieldUserApi customFieldUserApi;
    private ExportUserApi exportUserApi;
    private TagUserApi tagUserApi;
    private EntitlementApi entitlementApi;
    private SubscriptionApi subscriptionApi;
    private CurrencyConversionApi currencyConversionApi;
    private RecordIdApi recordIdApi;
    private SecurityApi securityApi;
    private PluginsInfoApi pluginsInfoApi;
    private KillbillNodesApi killbillNodesApi;
    private AdminPaymentApi adminPaymentApi;

    private PluginConfigServiceApi configServiceApi;

    @Inject(optional = true)
    public void setAccountUserApi(final AccountUserApi accountUserApi) {
        this.accountUserApi = accountUserApi;
    }

    @Inject(optional = true)
    public void setCatalogUserApi(final CatalogUserApi catalogUserApi) {
        this.catalogUserApi = catalogUserApi;
    }

    @Inject(optional = true)
    public void setInvoicePaymentApi(final InvoicePaymentApi invoicePaymentApi) {
        this.invoicePaymentApi = invoicePaymentApi;
    }

    @Inject(optional = true)
    public void setInvoiceUserApi(final InvoiceUserApi invoiceUserApi) {
        this.invoiceUserApi = invoiceUserApi;
    }

    @Inject(optional = true)
    public void setPaymentApi(final PaymentApi paymentApi) {
        this.paymentApi = paymentApi;
    }

    @Inject(optional = true)
    public void setTenantUserApi(final TenantUserApi tenantUserApi) {
        this.tenantUserApi = tenantUserApi;
    }

    @Inject(optional = true)
    public void setUsageUserApi(final UsageUserApi usageUserApi) {
        this.usageUserApi = usageUserApi;
    }

    @Inject(optional = true)
    public void setAuditUserApi(final AuditUserApi auditUserApi) {
        this.auditUserApi = auditUserApi;
    }

    @Inject(optional = true)
    public void setCustomFieldUserApi(final CustomFieldUserApi customFieldUserApi) {
        this.customFieldUserApi = customFieldUserApi;
    }

    @Inject(optional = true)
    public void setExportUserApi(final ExportUserApi exportUserApi) {
        this.exportUserApi = exportUserApi;
    }

    @Inject(optional = true)
    public void setTagUserApi(final TagUserApi tagUserApi) {
        this.tagUserApi = tagUserApi;
    }

    @Inject(optional = true)
    public void setEntitlementApi(final EntitlementApi entitlementApi) {
        this.entitlementApi = entitlementApi;
    }

    @Inject(optional = true)
    public void setSubscriptionApi(final SubscriptionApi subscriptionApi) {
        this.subscriptionApi = subscriptionApi;
    }

    @Inject(optional = true)
    public void setCurrencyConversionApi(final CurrencyConversionApi currencyConversionApi) {
        this.currencyConversionApi = currencyConversionApi;
    }

    @Inject(optional = true)
    public void setRecordIdApi(final RecordIdApi recordIdApi) {
        this.recordIdApi = recordIdApi;
    }

    @Inject(optional = true)
    public void setConfigServiceApi(final PluginConfigServiceApi configServiceApi) {
        this.configServiceApi = configServiceApi;
    }

    @Inject(optional = true)
    public void setSecurityApi(final SecurityApi securityApi) {
        this.securityApi = securityApi;
    }

    @Inject(optional = true)
    public void setPluginsInfoApi(final PluginsInfoApi pluginsInfoApi) {
        this.pluginsInfoApi = pluginsInfoApi;
    }

    @Inject(optional = true)
    public void setKillbillNodesApi(final KillbillNodesApi killbillNodesApi) {
        this.killbillNodesApi = killbillNodesApi;
    }

    @Inject(optional = true)
    public void setAdminPaymentApi(final AdminPaymentApi adminPaymentApi) {
        this.adminPaymentApi = adminPaymentApi;
    }

    @Override
    public AccountUserApi getAccountUserApi() {
        return accountUserApi;
    }

    @Override
    public CatalogUserApi getCatalogUserApi() {
        return catalogUserApi;
    }

    @Override
    public SubscriptionApi getSubscriptionApi() {
        return subscriptionApi;
    }

    @Override
    public InvoicePaymentApi getInvoicePaymentApi() {
        return invoicePaymentApi;
    }

    @Override
    public InvoiceUserApi getInvoiceUserApi() {
        return invoiceUserApi;
    }

    @Override
    public PaymentApi getPaymentApi() {
        return paymentApi;
    }

    @Override
    public TenantUserApi getTenantUserApi() {
        return tenantUserApi;
    }

    @Override
    public UsageUserApi getUsageUserApi() {
        return usageUserApi;
    }

    @Override
    public AuditUserApi getAuditUserApi() {
        return auditUserApi;
    }

    @Override
    public CustomFieldUserApi getCustomFieldUserApi() {
        return customFieldUserApi;
    }

    @Override
    public ExportUserApi getExportUserApi() {
        return exportUserApi;
    }

    @Override
    public TagUserApi getTagUserApi() {
        return tagUserApi;
    }

    @Override
    public EntitlementApi getEntitlementApi() {
        return entitlementApi;
    }

    @Override
    public RecordIdApi getRecordIdApi() {
        return recordIdApi;
    }

    @Override
    public CurrencyConversionApi getCurrencyConversionApi() {
        return currencyConversionApi;
    }

    @Override
    public PluginConfigServiceApi getPluginConfigServiceApi() {
        return configServiceApi;
    }

    @Override
    public SecurityApi getSecurityApi() {
        return securityApi;
    }

    @Override
    public PluginsInfoApi getPluginsInfoApi() {
        return pluginsInfoApi;
    }

    @Override
    public KillbillNodesApi getKillbillNodesApi() {
        return killbillNodesApi;
    }

    @Override
    public AdminPaymentApi getAdminPaymentApi() {
        return adminPaymentApi;
    }
}
