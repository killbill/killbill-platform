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

package org.killbill.billing.lifecycle;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import org.killbill.billing.lifecycle.DefaultLifecycle.LifecycleHandler;
import org.killbill.billing.platform.api.KillbillService;
import org.killbill.billing.platform.api.LifecycleHandlerType;
import org.killbill.billing.platform.api.LifecycleHandlerType.LifecycleLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class TestLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TestLifecycle.class);

    private Service1 s1;
    private Service2 s2;

    private DefaultLifecycle lifecycle;

    public static class ServiceBase {

        private int count = 0;

        public ServiceBase() {
            reset();
        }

        public synchronized void reset() {
            this.count = 0;
        }

        public synchronized int getCount() {
            return count;
        }

        public synchronized void incrementCount() {
            count++;
        }
    }

    public interface TestService1Interface extends KillbillService {

    }

    public static class Service1 extends ServiceBase implements TestService1Interface {

        @LifecycleHandlerType(LifecycleLevel.INIT_BUS)
        public void initBus() {
            log.info("Service1 : got INIT_BUS");
            incrementCount();
        }

        @LifecycleHandlerType(LifecycleLevel.START_SERVICE)
        public void startService() {
            log.info("Service1 : got START_SERVICE");
            incrementCount();
        }

        @LifecycleHandlerType(LifecycleLevel.SHUTDOWN)
        public void shutdownService() {
            log.info("Service1 : got SHUTDOWN");
            incrementCount();
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public int getRegistrationOrdering() {
            return 1000;
        }
    }

    public interface TestService2Interface extends KillbillService {
    }

    public static class Service2 extends ServiceBase implements TestService2Interface {

        @LifecycleHandlerType(LifecycleLevel.LOAD_CATALOG)
        public void loadCatalog() {
            log.info("Service2 : got LOAD_CATALOG");
            incrementCount();
        }

        @LifecycleHandlerType(LifecycleLevel.INIT_SERVICE)
        public void registerEvents() {
            log.info("Service2 : got REGISTER_EVENTS");
            incrementCount();
        }

        @LifecycleHandlerType(LifecycleLevel.STOP_SERVICE)
        public void unregisterEvents() {
            log.info("Service2 : got UNREGISTER_EVENTS");
            incrementCount();
        }

        @LifecycleHandlerType(LifecycleLevel.START_SERVICE)
        public void startService() {
            log.info("Service2 : got START_SERVICE");
            incrementCount();
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public int getRegistrationOrdering() {
            return 1000;
        }
    }


    public KillbillService createKillBillService(final String name, final int order) {
        return new KillbillService() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public int getRegistrationOrdering() {
                return order;
            }

            @LifecycleHandlerType(LifecycleLevel.INIT_SERVICE)
            public void initService() {
            }
        };
    }


    @BeforeClass(groups = "fast")
    public void setup() {
        final Injector g = Guice.createInjector(Stage.DEVELOPMENT, new TestLifecycleModule());
        s1 = g.getInstance(Service1.class);
        s2 = g.getInstance(Service2.class);

        final KillbillService s = createKillBillService("foo", 3);

        lifecycle = g.getInstance(DefaultLifecycle.class);
    }

    @Test(groups = "fast")
    public void testLifecycle() {
        s1.reset();
        s2.reset();
        lifecycle.fireStartupSequencePriorEventRegistration();
        Assert.assertEquals(s1.getCount() + s2.getCount(), 3);

        s1.reset();
        s2.reset();
        lifecycle.fireStartupSequencePostEventRegistration();
        Assert.assertEquals(s1.getCount() + s2.getCount(), 2);

        s1.reset();
        s2.reset();
        lifecycle.fireShutdownSequencePriorEventUnRegistration();
        Assert.assertEquals(s1.getCount() + s2.getCount(), 1);

        s1.reset();
        s2.reset();
        lifecycle.fireShutdownSequencePostEventUnRegistration();
        Assert.assertEquals(s1.getCount() + s2.getCount(), 1);
    }


    @Test(groups = "fast")
    public void testHandlersOrdering() {
        final Set<KillbillService> services = new HashSet();

        for (int i = 0; i < 100; i++) {
            int order = (i + 37) % 100;
            services.add(createKillBillService(String.format("yo-%d", order) , order));
        }

        final DefaultLifecycle otherLifecycle = new DefaultLifecycle(services);
        final SortedSet<LifecycleHandler<? extends KillbillService>> handlers =  otherLifecycle.getHandlersByLevel().get(LifecycleLevel.INIT_SERVICE);

        int prevOrdering = -1;
        for (LifecycleHandler<? extends KillbillService> h : handlers) {
            Assert.assertTrue(h.getTarget().getRegistrationOrdering() > prevOrdering);
            prevOrdering++;
        }

    }

    public static class LifecycleNoWarn extends DefaultLifecycle {

        @Inject
        public LifecycleNoWarn(final Injector injector) {
            super(injector);
        }

        @Override
        protected void logWarn(final String msg, final Exception e) {
        }
    }

    public static class TestLifecycleModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(DefaultLifecycle.class).to(LifecycleNoWarn.class).asEagerSingleton();
            bind(TestService1Interface.class).to(Service1.class);
            bind(Service1.class).asEagerSingleton();
            bind(Service2.class).asEagerSingleton();
            bind(TestService2Interface.class).to(Service2.class);
        }
    }
}

