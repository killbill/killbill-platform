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

package org.killbill.billing.platform.test.glue;

import org.killbill.billing.lifecycle.glue.BusModule;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.glue.MockNotificationQueueModule;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;

public class TestPlatformModuleNoDB extends TestPlatformModule {

    public TestPlatformModuleNoDB(final KillbillConfigSource configSource) {
        super(configSource, false, null, null);
    }

    @Override
    protected void configureEmbeddedDB() {
        final DBI dbi = Mockito.mock(DBI.class);
        // RETURNS_DEEP_STUBS doesn't work here
        Mockito.when(dbi.onDemand(Mockito.<Class<?>>any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                return Mockito.mock((Class<?>) invocation.getArguments()[0]);
            }
        });
        bind(DBI.class).toInstance(dbi);
        bind(IDBI.class).toInstance(dbi);
    }

    @Override
    protected void configureBus() {
        install(new BusModule(BusModule.BusType.MEMORY, false, configSource));
    }

    @Override
    protected void configureExternalBus() {
        install(new BusModule(BusModule.BusType.MEMORY, true, configSource));
    }

    @Override
    protected void configureNotificationQ() {
        install(new MockNotificationQueueModule(configSource));
    }
}
