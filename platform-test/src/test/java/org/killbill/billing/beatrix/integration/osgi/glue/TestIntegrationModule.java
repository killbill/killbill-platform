/*
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
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

import org.killbill.billing.catalog.plugin.api.CatalogPluginApi;
import org.killbill.billing.currency.plugin.api.CurrencyPluginApi;
import org.killbill.billing.entitlement.plugin.api.EntitlementPluginApi;
import org.killbill.billing.invoice.plugin.api.InvoicePluginApi;
import org.killbill.billing.osgi.api.OSGIServiceDescriptor;
import org.killbill.billing.osgi.api.OSGIServiceRegistration;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.glue.KillBillPlatformModuleBase;
import org.killbill.billing.platform.test.config.TestKillbillConfigSource;
import org.killbill.billing.platform.test.glue.TestPlatformModuleWithEmbeddedDB;
import org.killbill.billing.control.plugin.api.PaymentControlPluginApi;
import org.killbill.clock.Clock;
import org.killbill.clock.ClockMock;

import com.google.inject.TypeLiteral;

public class TestIntegrationModule extends KillBillPlatformModuleBase {

    public TestIntegrationModule(final KillbillConfigSource configSource) {
        super(configSource);
    }

    @Override
    protected void configure() {
        install(new TestPlatformModuleWithEmbeddedDB(configSource, true, (TestKillbillConfigSource) configSource));

        bind(Clock.class).to(ClockMock.class).asEagerSingleton();
        bind(new TypeLiteral<OSGIServiceRegistration<PaymentPluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<PaymentPluginApi>(PaymentPluginApi.class));
        bind(new TypeLiteral<OSGIServiceRegistration<CurrencyPluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<CurrencyPluginApi>(CurrencyPluginApi.class));
        bind(new TypeLiteral<OSGIServiceRegistration<InvoicePluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<InvoicePluginApi>(InvoicePluginApi.class));
        bind(new TypeLiteral<OSGIServiceRegistration<PaymentControlPluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<PaymentControlPluginApi>(PaymentControlPluginApi.class));
        bind(new TypeLiteral<OSGIServiceRegistration<CatalogPluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<CatalogPluginApi>(CatalogPluginApi.class));
        bind(new TypeLiteral<OSGIServiceRegistration<EntitlementPluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry<EntitlementPluginApi>(EntitlementPluginApi.class));
    }

    public static final class TestPlatformPaymentProviderPluginRegistry<T> implements OSGIServiceRegistration<T> {

        private final Map<String, T> pluginsByName = new ConcurrentHashMap<String, T>();

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
}
