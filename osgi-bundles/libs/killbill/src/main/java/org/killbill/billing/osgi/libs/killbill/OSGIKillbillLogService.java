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

package org.killbill.billing.osgi.libs.killbill;

import javax.annotation.Nullable;

import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillServiceReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.MDC;

// Plugins should be using slf4j directly
@Deprecated
public class OSGIKillbillLogService extends OSGIKillbillLibraryBase implements LogService {

    private static final String LOG_SERVICE_NAME = "org.osgi.service.log.LogService";

    private final ServiceTracker<LogService, LogService> logTracker;

    public OSGIKillbillLogService(final BundleContext context) {
        super();
        logTracker = new ServiceTracker<LogService, LogService>(context, LOG_SERVICE_NAME, null);
        logTracker.open();
    }

    public void close() {
        if (logTracker != null) {
            logTracker.close();
        }
    }

    @Override
    public void log(final int level, final String message) {
        logInternal(null, level, message, null);
    }

    @Override
    public void log(final int level, final String message, final Throwable exception) {
        logInternal(null, level, message, exception);
    }

    @Override
    public void log(final ServiceReference sr, final int level, final String message) {
        logInternal(sr, level, message, null);
    }

    @Override
    public void log(final ServiceReference sr, final int level, final String message, final Throwable exception) {
        logInternal(sr, level, message, exception);
    }

    private void logInternal(@Nullable final ServiceReference sr, final int level, final String message, @Nullable final Throwable t) {
        withServiceTracker(logTracker, new APICallback<Void, LogService>(LOG_SERVICE_NAME) {
            @Override
            public Void executeWithService(final LogService service) {
                final ServiceReference killbillServiceReference = new OSGIKillbillServiceReference(sr, MDC.getCopyOfContextMap());
                if (t == null) {
                    service.log(killbillServiceReference, level, message);
                } else {
                    service.log(killbillServiceReference, level, message, t);
                }
                return null;
            }

            protected Void executeWithNoService() {
                if (level >= 2) {
                    System.out.println(message);
                } else {
                    System.err.println(message);
                }
                if (t != null) {
                    t.printStackTrace(System.err);
                }
                return null;
            }
        });
    }

    @Override
    public Logger getLogger(final String name) {
        throw new java.lang.UnsupportedOperationException("Deprecated. Plugins should be using slf4j directly.");
    }

    @Override
    public Logger getLogger(final Class<?> clazz) {
        throw new java.lang.UnsupportedOperationException("Deprecated. Plugins should be using slf4j directly.");
    }

    @Override
    public <L extends Logger> L getLogger(final String name, final Class<L> loggerType) {
        throw new java.lang.UnsupportedOperationException("Deprecated. Plugins should be using slf4j directly.");
    }

    @Override
    public <L extends Logger> L getLogger(final Class<?> clazz, final Class<L> loggerType) {
        throw new java.lang.UnsupportedOperationException("Deprecated. Plugins should be using slf4j directly.");
    }

    @Override
    public <L extends Logger> L getLogger(final Bundle bundle, final String name, final Class<L> loggerType) {
        throw new java.lang.UnsupportedOperationException("Deprecated. Plugins should be using slf4j directly.");
    }
}
