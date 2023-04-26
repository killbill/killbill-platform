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

package org.killbill.billing.osgi.bundles.kpm;

import java.util.Hashtable;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.jooby.json.Jackson;
import org.killbill.billing.osgi.api.OSGIKillbillRegistrar;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.bundles.kpm.impl.DefaultPluginManager;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIFrameworkEventHandler;
import org.killbill.billing.plugin.core.resources.jooby.PluginApp;
import org.killbill.billing.plugin.core.resources.jooby.PluginAppBuilder;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends KillbillActivatorBase {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    public static final String PLUGIN_NAME = "killbill-kpm";

    private EventsListener eventsListener;

    @Override
    public void start(final BundleContext context) throws Exception {
        killbillAPI = new OSGIKillbillAPI(context);
        dispatcher = new OSGIKillbillEventDispatcher(context);
        configProperties = new OSGIConfigPropertiesService(context);
        registrar = new OSGIKillbillRegistrar();

        final Properties properties = configProperties.getProperties();
        final KpmProperties kpmProperties = new KpmProperties(properties);

        final DefaultPluginManager pluginManager = new DefaultPluginManager(killbillAPI, kpmProperties);
        eventsListener = new EventsListener(pluginManager);

        final Jackson jackson = new Jackson(PluginAppBuilder.DEFAULT_OBJECT_MAPPER);
        // JSON pass-through from KPM
        jackson.raw();
        final PluginApp pluginApp = new PluginAppBuilder(PLUGIN_NAME).withJackson(jackson)
                                                                     .withService(pluginManager)
                                                                     .withRouteClass(PluginsResource.class)
                                                                     .build();
        final HttpServlet httpServlet = PluginApp.createServlet(pluginApp);
        registerServlet(context, httpServlet);

        registerHandlers();
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
    }

    private void registerHandlers() {
        dispatcher.registerEventHandlers((OSGIFrameworkEventHandler) () -> dispatcher.registerEventHandlers(eventsListener));
    }

    private void registerServlet(final BundleContext context, final HttpServlet servlet) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Servlet.class, servlet, props);
    }
}
