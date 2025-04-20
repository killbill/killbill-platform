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

import java.util.Set;

import javax.annotation.Nullable;

import org.killbill.billing.lifecycle.DefaultLifecycle;
import org.killbill.billing.lifecycle.api.Lifecycle;
import org.killbill.billing.lifecycle.config.LifecycleConfig;
import org.killbill.billing.lifecycle.glue.BusModule;
import org.killbill.billing.osgi.api.OSGIConfigProperties;
import org.killbill.billing.osgi.api.PluginInfo;
import org.killbill.billing.osgi.glue.DefaultOSGIModule;
import org.killbill.billing.osgi.glue.OSGIDataSourceConfig;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.api.KillbillService;
import org.killbill.billing.platform.config.AugmentedConfigurationObjectFactory;
import org.killbill.billing.platform.glue.KillBillPlatformModuleBase;
import org.killbill.billing.platform.glue.NotificationQueueModule;
import org.killbill.billing.platform.jndi.JNDIManager;
import org.killbill.billing.platform.test.PlatformDBTestingHelper;
import org.killbill.billing.util.nodes.KillbillNodesApi;
import org.killbill.billing.util.nodes.NodeCommand;
import org.killbill.billing.util.nodes.NodeInfo;
import org.killbill.commons.embeddeddb.EmbeddedDB;

public abstract class TestPlatformModule extends KillBillPlatformModuleBase {

    private final boolean withOSGI;
    private final OSGIConfigProperties osgiConfigProperties;
    private final Set<? extends KillbillService> services;

    protected TestPlatformModule(final KillbillConfigSource configSource, final boolean withOSGI, @Nullable final OSGIConfigProperties osgiConfigProperties, @Nullable final Set<? extends KillbillService> services) {
        super(configSource);
        this.withOSGI = withOSGI;
        this.osgiConfigProperties = osgiConfigProperties;
        this.services = services;
    }

    @Override
    protected void configure() {
        configureEmbeddedDB();

        configureLifecycle();

        configureNotificationQ();

        configureBus();

        if (withOSGI) {
            configureExternalBus();

            configureOSGI();
        }

        configureJNDI();

        configureKillbillNodesApi();
    }

    protected abstract void configureEmbeddedDB();

    protected void configureLifecycle() {
        if (services != null) {
            bind(Lifecycle.class).toInstance(new DefaultLifecycle(services));
        } else {
            bind(Lifecycle.class).to(DefaultLifecycle.class).asEagerSingleton();
        }
        final LifecycleConfig lifecycleConfig = new AugmentedConfigurationObjectFactory(skifeConfigSource).build(LifecycleConfig.class);
        bind(LifecycleConfig.class).toInstance(lifecycleConfig);
    }

    protected void configureBus() {
        install(new BusModule(BusModule.BusType.PERSISTENT, false, configSource));
    }

    protected void configureExternalBus() {
        install(new BusModule(BusModule.BusType.PERSISTENT, true, configSource));
    }

    protected void configureNotificationQ() {
        install(new NotificationQueueModule(configSource));
    }

    protected void configureOSGI() {
        final OSGIDataSourceConfig osgiDataSourceConfig = new AugmentedConfigurationObjectFactory(skifeConfigSource).build(OSGIDataSourceConfig.class);
        final EmbeddedDB embeddedDB = PlatformDBTestingHelper.get().getInstance();
        install(new DefaultOSGIModule(configSource, osgiConfigProperties, osgiDataSourceConfig, embeddedDB));
    }

    protected void configureJNDI() {
        bind(JNDIManager.class).asEagerSingleton();
    }

    protected void configureKillbillNodesApi() {
        final KillbillNodesApi dummyInstance = new KillbillNodesApi() {
            @Override
            public Iterable<NodeInfo> getNodesInfo() {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public NodeInfo getCurrentNodeInfo() {
                return null;
            }

            @Override
            public void triggerNodeCommand(final NodeCommand nodeCommandfinal, final boolean localNodeOnly) {
                throw new UnsupportedOperationException("Not implemented");
            }
            @Override
            public void notifyPluginChanged(final PluginInfo pluginInfo, final Iterable<PluginInfo> latestPlugins) {
                throw new UnsupportedOperationException("Not implemented");
            }
        };
        bind(KillbillNodesApi.class).toInstance(dummyInstance);
    }
}
