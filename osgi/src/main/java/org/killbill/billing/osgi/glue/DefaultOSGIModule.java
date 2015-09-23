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

package org.killbill.billing.osgi.glue;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.sql.DataSource;

import org.killbill.billing.osgi.DefaultOSGIKillbill;
import org.killbill.billing.osgi.DefaultOSGIService;
import org.killbill.billing.osgi.KillbillActivator;
import org.killbill.billing.osgi.KillbillEventObservable;
import org.killbill.billing.osgi.OSGIMetricsFactory;
import org.killbill.billing.osgi.PureOSGIBundleFinder;
import org.killbill.billing.osgi.api.OSGIConfigProperties;
import org.killbill.billing.osgi.api.OSGIKillbill;
import org.killbill.billing.osgi.api.OSGIServiceRegistration;
import org.killbill.billing.osgi.api.config.PluginConfigServiceApi;
import org.killbill.billing.osgi.config.OSGIConfig;
import org.killbill.billing.osgi.http.DefaultHttpService;
import org.killbill.billing.osgi.http.DefaultServletRouter;
import org.killbill.billing.osgi.http.OSGIServlet;
import org.killbill.billing.osgi.pluginconf.DefaultPluginConfigServiceApi;
import org.killbill.billing.osgi.pluginconf.PluginFinder;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.api.OSGIService;
import org.killbill.billing.platform.glue.KillBillPlatformModuleBase;
import org.killbill.billing.platform.glue.ReferenceableDataSourceSpyProvider;
import org.killbill.commons.jdbi.guice.DaoConfig;
import org.osgi.service.http.HttpService;
import org.skife.config.ConfigurationObjectFactory;

import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class DefaultOSGIModule extends KillBillPlatformModuleBase {

    public static final String OSGI_NAMED = "osgi";

    private final OSGIConfigProperties osgiConfigProperties;

    public DefaultOSGIModule(final KillbillConfigSource configSource, final OSGIConfigProperties osgiConfigProperties) {
        super(configSource);
        this.osgiConfigProperties = osgiConfigProperties;
    }

    protected void installConfig() {
        final OSGIConfig config = new ConfigurationObjectFactory(skifeConfigSource).build(OSGIConfig.class);
        bind(OSGIConfig.class).toInstance(config);
        bind(OSGIConfigProperties.class).toInstance(osgiConfigProperties);
    }

    protected void installOSGIServlet() {
        bind(new TypeLiteral<OSGIServiceRegistration<Servlet>>() {
        }).to(DefaultServletRouter.class).asEagerSingleton();
        bind(HttpServlet.class).annotatedWith(Names.named(OSGI_NAMED)).to(OSGIServlet.class).asEagerSingleton();
    }

    protected void installDataSource() {
        final OSGIDataSourceConfig osgiDataSourceConfig = new ConfigurationObjectFactory(skifeConfigSource).build(OSGIDataSourceConfig.class);
        bind(OSGIDataSourceConfig.class).toInstance(osgiDataSourceConfig);
        bind(DaoConfig.class).annotatedWith(Names.named(OSGI_DATA_SOURCE_ID_NAMED)).toInstance(osgiDataSourceConfig);

        final Provider<DataSource> dataSourceSpyProvider = new ReferenceableDataSourceSpyProvider(osgiDataSourceConfig, OSGI_DATA_SOURCE_ID_NAMED);
        requestInjection(dataSourceSpyProvider);
        bind(DataSource.class).annotatedWith(Names.named(OSGI_DATA_SOURCE_ID_NAMED)).toProvider(dataSourceSpyProvider).asEagerSingleton();
    }

    protected void installHttpService() {
        bind(HttpService.class).to(DefaultHttpService.class).asEagerSingleton();
    }

    @Override
    protected void configure() {
        installConfig();
        installOSGIServlet();
        installHttpService();
        installDataSource();

        bind(OSGIMetricsFactory.class);

        bind(OSGIService.class).to(DefaultOSGIService.class).asEagerSingleton();

        bind(KillbillActivator.class).asEagerSingleton();
        bind(PureOSGIBundleFinder.class).asEagerSingleton();
        bind(PluginFinder.class).asEagerSingleton();
        bind(PluginConfigServiceApi.class).to(DefaultPluginConfigServiceApi.class).asEagerSingleton();
        bind(OSGIKillbill.class).to(DefaultOSGIKillbill.class).asEagerSingleton();
        bind(KillbillEventObservable.class).asEagerSingleton();
    }
}
