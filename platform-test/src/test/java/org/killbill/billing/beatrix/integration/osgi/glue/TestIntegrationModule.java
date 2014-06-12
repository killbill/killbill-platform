/*
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

package org.killbill.billing.beatrix.integration.osgi.glue;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.killbill.billing.osgi.api.OSGIServiceDescriptor;
import org.killbill.billing.osgi.api.OSGIServiceRegistration;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.glue.KillBillModule;
import org.killbill.billing.platform.test.config.TestKillbillConfigSource;
import org.killbill.billing.platform.test.glue.TestPlatformModuleWithEmbeddedDB;
import org.killbill.clock.Clock;
import org.killbill.clock.ClockMock;

import com.google.inject.TypeLiteral;

public class TestIntegrationModule extends KillBillModule {

    public TestIntegrationModule(final KillbillConfigSource configSource) {
        super(configSource);
    }

    @Override
    protected void configure() {
        install(new TestPlatformModuleWithEmbeddedDB(configSource, true, (TestKillbillConfigSource) configSource));

        bind(Clock.class).to(ClockMock.class).asEagerSingleton();
        bind(TestApiListener.class).asEagerSingleton();
        bind(new TypeLiteral<OSGIServiceRegistration<PaymentPluginApi>>() {}).toInstance(new TestPlatformPaymentProviderPluginRegistry());
    }

    public static final class TestPlatformPaymentProviderPluginRegistry implements OSGIServiceRegistration<PaymentPluginApi> {

        private final Map<String, PaymentPluginApi> pluginsByName = new ConcurrentHashMap<String, PaymentPluginApi>();

        @Override
        public void registerService(final OSGIServiceDescriptor desc, final PaymentPluginApi service) {
            pluginsByName.put(desc.getRegistrationName(), service);
        }

        @Override
        public void unregisterService(final String serviceName) {
            pluginsByName.remove(serviceName);
        }

        @Override
        public PaymentPluginApi getServiceForName(final String name) {
            return pluginsByName.get(name);
        }

        @Override
        public Set<String> getAllServices() {
            return pluginsByName.keySet();
        }

        @Override
        public Class<PaymentPluginApi> getServiceType() {
            return PaymentPluginApi.class;
        }
    }
}
