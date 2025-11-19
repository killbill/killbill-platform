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

package org.killbill.billing.beatrix.integration.osgi;

import java.util.Properties;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.awaitility.Awaitility;
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
import org.skife.config.RuntimeConfigRegistry;
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

    protected TestKillbillConfigSource configSource;
    protected CallContext callContext;

    public TestOSGIBase() {
        System.out.println("TestOSGIBase is called...");
        callContext = Mockito.mock(CallContext.class);
    }

    @BeforeSuite(groups = "slow")
    public void beforeSuite() throws Exception {

        System.out.println("TestOSGIBase beforeSuite is called....");

       /* if (System.getProperty("org.killbill.billing.dbi.test.h2") == null && System.getProperty("org.killbill.billing.dbi.test.postgresql") == null) {
            System.setProperty("org.killbill.billing.dbi.test.h2", "true");
        }*/

        RuntimeConfigRegistry.clear();

        try {
            configSource = new TestKillbillConfigSource(null, PlatformDBTestingHelper.class);

        } catch (final Exception e) {
            final AssertionError assertionError = new AssertionError("Initialization error");
            assertionError.initCause(e);
            throw assertionError;
        }

        System.out.println("=== DEBUG: ALL OSGI DAO PROPERTIES ===");
        Properties allProps = configSource.getProperties();
        System.out.println("Total properties: " + allProps.size());
        for (String key : allProps.stringPropertyNames()) {
            if (key.contains("osgi.dao")) {
                System.out.println("  " + key + " = " + allProps.getProperty(key));
            }
        }
        System.out.println("=== END DEBUG ===");

        PlatformDBTestingHelper.get().start();

        System.out.println("BeforeSuite --- final resolved properties");
        System.out.println("Current7 values in getProperties...");
        configSource.getProperties().forEach((object, object2) -> System.out.println(object + ":  " + object2));

        System.out.println("Current7 values in getPropertiesBySource...");
        configSource.getPropertiesBySource().forEach((s, stringStringMap) -> {
            System.out.println(s);
            stringStringMap.forEach((s1, s2) -> System.out.println("  " + s1 + ": " + s2));
        });
    }

    @BeforeClass(groups = "slow")
    public void beforeClass() throws Exception {
        // configSource.test();
        /*try {
            RuntimeConfigRegistry.clear();
            configSource = new TestKillbillConfigSource(null, PlatformDBTestingHelper.class);
        } catch (final Exception e) {
            final AssertionError assertionError = new AssertionError("Initialization error in beforeClass");
            assertionError.initCause(e);
            throw assertionError;
        }*/

       /* if (configSource == null && System.getProperty("_test_config_source_created") != null) {
            configSource = new TestKillbillConfigSource(null, PlatformDBTestingHelper.class);
        }
*/

        System.out.println("TestOSGIBase beforeClass is called....");

        if (configSource == null) {
            try {
                System.out.println("configSource... is null in TestOSGIBase beforeClass");
                //RuntimeConfigRegistry.clear();
                configSource = new TestKillbillConfigSource(null, PlatformDBTestingHelper.class);
            } catch (final Exception e) {
                throw new AssertionError("Failed to create configSource", e);
            }
        }

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
            ((HikariDataSource) ((ReferenceableDataSourceSpy) dataSource).getDataSource()).close();
        }
        if (osgiDataSource instanceof ReferenceableDataSourceSpy && ((ReferenceableDataSourceSpy) osgiDataSource).getDataSource() instanceof HikariDataSource) {
            ((HikariDataSource) ((ReferenceableDataSourceSpy) osgiDataSource).getDataSource()).close();
        }
    }

    protected void ensureConfigSource() {
        if (configSource == null) {
            //throw new AssertionError("configSource is null - @BeforeSuite must have failed or didn't run on this test instance");

            RuntimeConfigRegistry.clear();

            try {
                configSource = new TestKillbillConfigSource(null, PlatformDBTestingHelper.class);

            } catch (final Exception e) {
                final AssertionError assertionError = new AssertionError("Initialization error");
                assertionError.initCause(e);
                throw assertionError;
            }
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

                return serviceRegistration.getServiceForName(serviceName) != null;
            }
        });

        return serviceRegistration.getServiceForName(serviceName);
    }
}