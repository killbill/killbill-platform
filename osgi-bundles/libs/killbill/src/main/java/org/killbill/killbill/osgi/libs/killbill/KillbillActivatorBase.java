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

package org.killbill.killbill.osgi.libs.killbill;

import org.killbill.billing.osgi.api.OSGIKillbillRegistrar;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public abstract class KillbillActivatorBase implements BundleActivator {

    protected OSGIKillbillAPI killbillAPI;
    protected OSGIKillbillLogService logService;
    protected OSGIKillbillRegistrar registrar;
    protected OSGIKillbillDataSource dataSource;
    protected OSGIKillbillEventDispatcher dispatcher;
    protected OSGIConfigPropertiesService configProperties;
    protected OSGIKillbillMetrics killbillMetrics;

    @Override
    public void start(final BundleContext context) throws Exception {
        // Tracked resource
        killbillAPI = new OSGIKillbillAPI(context);
        logService = new OSGIKillbillLogService(context);
        dataSource = new OSGIKillbillDataSource(context);
        dispatcher = new OSGIKillbillEventDispatcher(context);
        configProperties = new OSGIConfigPropertiesService(context);
        killbillMetrics = new OSGIKillbillMetrics(context);

        // Registrar for bundle
        registrar = new OSGIKillbillRegistrar();

        // Killbill events
        final OSGIKillbillEventHandler handler = getOSGIKillbillEventHandler();
        if (handler != null) {
            dispatcher.registerEventHandler(handler);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        // Close trackers
        if (killbillAPI != null) {
            killbillAPI.close();
            killbillAPI = null;
        }
        if (dispatcher != null) {
            dispatcher.close();
            dispatcher = null;
        }
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
        if (logService != null) {
            logService.close();
            logService = null;
        }
        if (killbillMetrics != null) {
            killbillMetrics.close();
            killbillMetrics = null;
        }

        try {
            // Remove Killbill event handler
            final OSGIKillbillEventHandler handler = getOSGIKillbillEventHandler();
            if (handler != null && dispatcher != null) {
                dispatcher.unregisterEventHandler(handler);
                dispatcher = null;
            }
        } catch (final OSGIServiceNotAvailable ignore) {
            // If the system bundle shut down prior to that bundle, we can' unregister our Observer, which is fine.
        }

        // Unregister all services from that bundle
        if (registrar != null) {
            registrar.unregisterAll();
            registrar = null;
        }
    }

    public abstract OSGIKillbillEventHandler getOSGIKillbillEventHandler();
}
