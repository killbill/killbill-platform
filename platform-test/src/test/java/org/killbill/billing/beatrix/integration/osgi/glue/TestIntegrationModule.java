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

package org.killbill.billing.beatrix.integration.osgi.glue;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.killbill.billing.account.api.AccountUserApi;
import org.killbill.billing.catalog.api.CatalogUserApi;
import org.killbill.billing.catalog.plugin.api.CatalogPluginApi;
import org.killbill.billing.control.plugin.api.PaymentControlPluginApi;
import org.killbill.billing.currency.api.CurrencyConversionApi;
import org.killbill.billing.currency.plugin.api.CurrencyPluginApi;
import org.killbill.billing.entitlement.api.EntitlementApi;
import org.killbill.billing.entitlement.api.SubscriptionApi;
import org.killbill.billing.entitlement.plugin.api.EntitlementPluginApi;
import org.killbill.billing.invoice.api.InvoiceUserApi;
import org.killbill.billing.invoice.plugin.api.InvoicePluginApi;
import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.osgi.api.OSGIServiceDescriptor;
import org.killbill.billing.osgi.api.OSGIServiceRegistration;
import org.killbill.billing.osgi.api.OSGISingleServiceRegistration;
import org.killbill.billing.osgi.api.ServiceDiscoveryRegistry;
import org.killbill.billing.overdue.api.OverdueApi;
import org.killbill.billing.payment.api.AdminPaymentApi;
import org.killbill.billing.payment.api.InvoicePaymentApi;
import org.killbill.billing.payment.api.PaymentApi;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.glue.KillBillPlatformModuleBase;
import org.killbill.billing.platform.test.config.TestKillbillConfigSource;
import org.killbill.billing.platform.test.glue.TestPlatformModuleWithEmbeddedDB;
import org.killbill.billing.security.api.SecurityApi;
import org.killbill.billing.tenant.api.TenantUserApi;
import org.killbill.billing.usage.api.UsageUserApi;
import org.killbill.billing.usage.plugin.api.UsagePluginApi;
import org.killbill.billing.util.api.AuditUserApi;
import org.killbill.billing.util.api.CustomFieldUserApi;
import org.killbill.billing.util.api.ExportUserApi;
import org.killbill.billing.util.api.RecordIdApi;
import org.killbill.billing.util.api.TagUserApi;
import org.killbill.clock.Clock;
import org.killbill.clock.ClockMock;
import org.killbill.commons.health.api.HealthCheckRegistry;
import org.killbill.commons.metrics.api.MetricRegistry;
import org.killbill.commons.metrics.impl.NoOpMetricRegistry;

import com.google.inject.TypeLiteral;
import com.google.inject.util.Providers;

public class TestIntegrationModule extends KillBillPlatformModuleBase {

    public TestIntegrationModule(final KillbillConfigSource configSource) {
        super(configSource);
    }

    @Override
    protected void configure() {
        install(new TestPlatformModuleWithEmbeddedDB(configSource, true, (TestKillbillConfigSource) configSource));

        bind(MetricRegistry.class).to(NoOpMetricRegistry.class);
        bind(HealthCheckRegistry.class).toProvider(Providers.of(null));
        bind(Clock.class).to(ClockMock.class);
        // Make sure we have a unique clock if one requests ClockMock explicitly
        bind(ClockMock.class).asEagerSingleton();

        bindOsgiKillbill();

        bindOsgiServiceRegistration();
    }

    // These API skipped because registered in other module:
    // - PluginsInfoApi in DefaultOSGIModule
    // - KillbillNodesApi in TestPlatformModule
    // - PluginConfigServiceApi in DefaultOSGIModule
    protected void bindOsgiKillbill() {
        bind(AccountUserApi.class).toProvider(Providers.of(null));
        bind(CatalogUserApi.class).toProvider(Providers.of(null));
        bind(InvoicePaymentApi.class).toProvider(Providers.of(null));
        bind(InvoiceUserApi.class).toProvider(Providers.of(null));
        bind(PaymentApi.class).toProvider(Providers.of(null));
        bind(TenantUserApi.class).toProvider(Providers.of(null));
        bind(UsageUserApi.class).toProvider(Providers.of(null));
        bind(AuditUserApi.class).toProvider(Providers.of(null));
        bind(CustomFieldUserApi.class).toProvider(Providers.of(null));
        bind(ExportUserApi.class).toProvider(Providers.of(null));
        bind(TagUserApi.class).toProvider(Providers.of(null));
        bind(EntitlementApi.class).toProvider(Providers.of(null));
        bind(SubscriptionApi.class).toProvider(Providers.of(null));
        bind(CurrencyConversionApi.class).toProvider(Providers.of(null));
        bind(RecordIdApi.class).toProvider(Providers.of(null));
        bind(SecurityApi.class).toProvider(Providers.of(null));
        bind(AdminPaymentApi.class).toProvider(Providers.of(null));
        bind(OverdueApi.class).toProvider(Providers.of(null));
    }

    protected void bindOsgiServiceRegistration() {
        bind(new TypeLiteral<OSGIServiceRegistration<PaymentPluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<>(PaymentPluginApi.class));
        bind(new TypeLiteral<OSGIServiceRegistration<CurrencyPluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<>(CurrencyPluginApi.class));
        bind(new TypeLiteral<OSGIServiceRegistration<InvoicePluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<>(InvoicePluginApi.class));
        bind(new TypeLiteral<OSGIServiceRegistration<PaymentControlPluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<>(PaymentControlPluginApi.class));
        bind(new TypeLiteral<OSGIServiceRegistration<CatalogPluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<>(CatalogPluginApi.class));
        bind(new TypeLiteral<OSGIServiceRegistration<EntitlementPluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<>(EntitlementPluginApi.class));
        bind(new TypeLiteral<OSGIServiceRegistration<Healthcheck>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<>(Healthcheck.class));
        bind(new TypeLiteral<OSGIServiceRegistration<ServiceDiscoveryRegistry>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<>(ServiceDiscoveryRegistry.class));
        bind(new TypeLiteral<OSGIServiceRegistration<UsagePluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<>(UsagePluginApi.class));
        bind(new TypeLiteral<OSGISingleServiceRegistration<MetricRegistry>>() {}).toInstance(new TestOSGISingleServiceRegistration<>(MetricRegistry.class));
    }

    public static final class TestPlatformPaymentProviderPluginRegistry<T> implements OSGIServiceRegistration<T> {

        private final Map<String, T> pluginsByName = new ConcurrentHashMap<>();

        private final Class<T> serviceType;

        public TestPlatformPaymentProviderPluginRegistry(final Class<T> serviceType) {
            this.serviceType = serviceType;
        }

        @Override
        public void registerService(final OSGIServiceDescriptor desc, final T service) {
            pluginsByName.put(desc.getRegistrationName(), service);
        }

        @Override
        public void unregisterService(final String serviceName) {
            pluginsByName.remove(serviceName);
        }

        @Override
        public T getServiceForName(final String name) {
            return pluginsByName.get(name);
        }

        @Override
        public Set<String> getAllServices() {
            return pluginsByName.keySet();
        }

        @Override
        public Class<T> getServiceType() {
            return serviceType;
        }
    }

    static final class TestOSGISingleServiceRegistration<T> implements OSGISingleServiceRegistration<T> {

        private final Map<String, T> pluginsByName = new ConcurrentHashMap<>();

        private final Class<T> serviceType;

        public TestOSGISingleServiceRegistration(final Class<T> serviceType) {
            this.serviceType = serviceType;
        }

        @Override
        public void registerService(final OSGIServiceDescriptor desc, final T service) {
            pluginsByName.put(desc.getPluginName(), service);
        }

        @Override
        public void unregisterService(final String serviceName) {
            pluginsByName.remove(serviceName);
        }

        @Override
        public Class<T> getServiceType() {
            return serviceType;
        }

        @Override
        public T getService() {
            return this.pluginsByName.values().stream().findFirst().orElse(null);
        }
    }
}
