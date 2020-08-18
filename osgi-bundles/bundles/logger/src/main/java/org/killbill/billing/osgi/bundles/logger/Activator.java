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

package org.killbill.billing.osgi.bundles.logger;

import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.killbill.billing.osgi.api.OSGIKillbillRegistrar;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.plugin.core.resources.jooby.PluginApp;
import org.killbill.billing.plugin.core.resources.jooby.PluginAppBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends KillbillActivatorBase {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    public static final String PLUGIN_NAME = "killbill-osgi-logger";

    private LogEntriesManager logEntriesManager;
    private LogsSseHandler logsSseHandler;
    private KillbillLogWriter killbillLogListener;

    @Override
    public void start(final BundleContext context) throws Exception {
        logEntriesManager = new LogEntriesManager();
        killbillLogListener = new KillbillLogWriter(logEntriesManager);
        context.addBundleListener(killbillLogListener);
        context.addFrameworkListener(killbillLogListener);
        context.addServiceListener(killbillLogListener);
        context.registerService(LogService.class.getName(), killbillLogListener, null);

        // Registrar for bundle
        registrar = new OSGIKillbillRegistrar();

        final PluginApp pluginApp = new PluginAppBuilder(PLUGIN_NAME).build();
        logsSseHandler = new LogsSseHandler(logEntriesManager);
        pluginApp.sse("/", logsSseHandler);
        final HttpServlet httpServlet = PluginApp.createServlet(pluginApp);
        registerServlet(context, httpServlet);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (logsSseHandler != null) {
            logsSseHandler.close();
        }

        super.stop(context);
    }

    private void registerServlet(final BundleContext context, final HttpServlet servlet) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Servlet.class, servlet, props);
    }
}
