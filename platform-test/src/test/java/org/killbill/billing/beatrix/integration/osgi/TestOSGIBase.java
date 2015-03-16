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

package org.killbill.billing.beatrix.integration.osgi;

import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.killbill.billing.beatrix.integration.osgi.glue.TestIntegrationModule;
import org.killbill.billing.currency.plugin.api.CurrencyPluginApi;
import org.killbill.billing.lifecycle.api.BusService;
import org.killbill.billing.lifecycle.api.Lifecycle;
import org.killbill.billing.lifecycle.glue.BusModule;
import org.killbill.billing.osgi.api.OSGIServiceRegistration;
import org.killbill.billing.osgi.config.OSGIConfig;
import org.killbill.billing.osgi.glue.DefaultOSGIModule;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.platform.jndi.ReferenceableDataSourceSpy;
import org.killbill.billing.platform.test.PlatformDBTestingHelper;
import org.killbill.billing.platform.test.config.TestKillbillConfigSource;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.bus.api.PersistentBus;
import org.killbill.clock.ClockMock;
import org.mockito.Mockito;
import org.skife.jdbi.v2.IDBI;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.name.Named;
import com.jayway.awaitility.Awaitility;
import com.zaxxer.hikari.HikariDataSource;

public class TestOSGIBase {

    @Inject
    protected Lifecycle lifecycle;

    @Inject
    protected BusService busService;

    @Inject
    @Named(BusModule.EXTERNAL_BUS_NAMED)
    protected PersistentBus externalBus;

    @Inject
    protected IDBI dbi;

    @Inject
    protected DataSource dataSource;

    @Inject
    @Named(DefaultOSGIModule.OSGI_NAMED)
    protected DataSource osgiDataSource;

    @Inject
    protected OSGIConfig osgiConfig;

    @Inject
    protected ClockMock clock;

    @Inject
    protected OSGIServiceRegistration<PaymentPluginApi> paymentPluginApiOSGIServiceRegistration;

    @Inject
    protected OSGIServiceRegistration<CurrencyPluginApi> currencyPluginApiOSGIServiceRegistration;

    protected final TestKillbillConfigSource configSource;
    protected CallContext callContext;

    public TestOSGIBase() {
        try {
            configSource = new TestKillbillConfigSource("/beatrix.properties", PlatformDBTestingHelper.class);
        } catch (final Exception e) {
            final AssertionError assertionError = new AssertionError("Initialization error");
            assertionError.initCause(e);
            throw assertionError;
        }

        callContext = Mockito.mock(CallContext.class);
    }

    @BeforeSuite(groups = "slow")
    public void beforeSuite() throws Exception {
        PlatformDBTestingHelper.get().start();
    }

    @BeforeClass(groups = "slow")
    public void beforeClass() throws Exception {
        final Injector g = Guice.createInjector(Stage.PRODUCTION, new TestIntegrationModule(configSource));
        g.injectMembers(this);
    }

    @BeforeMethod(groups = "slow")
    public void beforeMethod() throws Exception {
        try {
            PlatformDBTestingHelper.get().getInstance().cleanupAllTables();
        } catch (final Exception ignored) {
        }

        clock.resetDeltaFromReality();

        // Start services
        lifecycle.fireStartupSequencePriorEventRegistration();
        lifecycle.fireStartupSequencePostEventRegistration();
    }

    @AfterMethod(groups = "slow")
    public void afterMethod() throws Exception {
        lifecycle.fireShutdownSequencePriorEventUnRegistration();
        lifecycle.fireShutdownSequencePostEventUnRegistration();
    }

    @AfterClass(groups = "slow")
    public void afterClass() throws Exception {
        if (dataSource instanceof ReferenceableDataSourceSpy && ((ReferenceableDataSourceSpy) dataSource).getDataSource() instanceof HikariDataSource) {
            ((HikariDataSource) ((ReferenceableDataSourceSpy) dataSource).getDataSource()).shutdown();
        }
        if (osgiDataSource instanceof ReferenceableDataSourceSpy && ((ReferenceableDataSourceSpy) osgiDataSource).getDataSource() instanceof HikariDataSource) {
            ((HikariDataSource) ((ReferenceableDataSourceSpy) osgiDataSource).getDataSource()).shutdown();
        }
    }

    @AfterSuite(groups = "slow")
    public void afterSuite() throws Exception {
        try {
            PlatformDBTestingHelper.get().getInstance().stop();
        } catch (final Exception ignored) {
        }
    }

    protected <T> T getTestApi(final OSGIServiceRegistration<T> serviceRegistration, final String serviceName) throws Exception {
        Awaitility.await().until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // It is expected to have a null result if the initialization of Killbill went faster than the registration of the plugin services
                return serviceRegistration.getServiceForName(serviceName) != null;
            }
        });

        return serviceRegistration.getServiceForName(serviceName);
    }
}
