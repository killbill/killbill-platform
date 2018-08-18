/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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

package org.killbill.billing.osgi.bundles.rpc;

import org.killbill.billing.osgi.bundles.rpc.server.KillBillGRPCServer;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.killbill.billing.osgi.bundles.rpc.server.KillBillGRPCServer.DEFAULT_SERVER_PORT;

public class Activator extends KillbillActivatorBase {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    public static final String PLUGIN_NAME = "killbill-osgi-rpc";

    private KillBillGRPCServer gRPCServer;

    public void start(final BundleContext context) throws Exception {
        super.start(context);
        logger.info("Starting bundle " + PLUGIN_NAME);
        gRPCServer = new KillBillGRPCServer(DEFAULT_SERVER_PORT, context, registrar);
        gRPCServer.start();
    }

    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        logger.info("Stopping bundle " + PLUGIN_NAME);
        gRPCServer.stop();
    }
}