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

import javax.inject.Inject;

import org.killbill.billing.beatrix.integration.osgi.glue.TestApiListener;
import org.killbill.billing.beatrix.integration.osgi.glue.TestIntegrationModule;
import org.killbill.billing.lifecycle.api.BusService;
import org.killbill.billing.lifecycle.api.Lifecycle;
import org.killbill.billing.lifecycle.glue.BusModule;
import org.killbill.billing.osgi.config.OSGIConfig;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.test.PlatformDBTestingHelper;
import org.killbill.billing.platform.test.config.TestKillbillConfigSource;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.bus.api.PersistentBus;
import org.killbill.clock.ClockMock;
import org.mockito.Mockito;
import org.skife.jdbi.v2.IDBI;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.name.Named;

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
    protected OSGIConfig osgiConfig;

    @Inject
    protected ClockMock clock;

    protected final KillbillConfigSource configSource;
    protected CallContext callContext;
    protected TestApiListener busHandler;

    public TestOSGIBase() {
        try {
            configSource = new TestKillbillConfigSource("/beatrix.properties", PlatformDBTestingHelper.get().getInstance().getJdbcConnectionString(),
                                                        PlatformDBTestingHelper.get().getInstance().getUsername(), PlatformDBTestingHelper.get().getInstance().getPassword());
        } catch (final Exception e) {
            final AssertionError assertionError = new AssertionError("Initialization error");
            assertionError.initCause(e);
            throw assertionError;
        }

        callContext = Mockito.mock(CallContext.class);
        busHandler = new TestApiListener();
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
        busHandler.reset();

        // Start services
        lifecycle.fireStartupSequencePriorEventRegistration();
        busService.getBus().register(busHandler);
        lifecycle.fireStartupSequencePostEventRegistration();

        // Make sure we start with a clean state
        busHandler.assertListenerStatus();
    }

    @AfterMethod(groups = "slow")
    public void afterMethod() throws Exception {
        // Make sure we finish in a clean state
        busHandler.assertListenerStatus();

        lifecycle.fireShutdownSequencePriorEventUnRegistration();
        busService.getBus().unregister(busHandler);
        lifecycle.fireShutdownSequencePostEventUnRegistration();
    }

    @AfterSuite(groups = "slow")
    public void afterSuite() throws Exception {
        try {
            PlatformDBTestingHelper.get().getInstance().stop();
        } catch (final Exception ignored) {
        }
    }
}
