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

package org.killbill.billing.osgi;

import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class OSGIAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private final ServiceTracker<LogService, LogService> logTracker;
    private final ServiceReference SR;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OSGIAppender(final ServiceTracker<LogService, LogService> logTracker, final Bundle bundle) {
        this.logTracker = logTracker;
        this.SR = new RootBundleLogbackServiceReference(bundle);
    }

    @Override
    protected void append(final ILoggingEvent eventObject) {
        final LogService logService = logTracker.getService();
        if (logService == null) {
            return;
        }

        final int level;
        switch (eventObject.getLevel().toInt()) {
            case Level.TRACE_INT:
                level = LogService.LOG_DEBUG;
                break;
            case Level.DEBUG_INT:
                level = LogService.LOG_DEBUG;
                break;
            case Level.INFO_INT:
                level = LogService.LOG_INFO;
                break;
            case Level.WARN_INT:
                level = LogService.LOG_WARNING;
                break;
            case Level.ERROR_INT:
                level = LogService.LOG_ERROR;
                break;
            default:
                level = LogService.LOG_DEBUG;
                break;
        }

        Throwable t = null;
        if (eventObject.getThrowableProxy() != null) {
            if (eventObject.getThrowableProxy() instanceof ThrowableProxy) {
                t = ((ThrowableProxy) eventObject.getThrowableProxy()).getThrowable();
            } else {
                t = new Throwable(eventObject.getThrowableProxy().getMessage());
            }
        }

        logService.log(SR, level, eventObject.getFormattedMessage(), t);
    }

    private static final class RootBundleLogbackServiceReference implements ServiceReference {

        // MAGIC - do not change (see KillbillLogWriter)
        private static final Map<String, String> SERVICE_KEYS = ImmutableMap.<String, String>of("KILL_BILL_ROOT_LOGGING", "true");

        private final Bundle bundle;

        public RootBundleLogbackServiceReference(final Bundle bundle) {
            this.bundle = bundle;
        }

        @Override
        public Object getProperty(final String key) {
            return SERVICE_KEYS.get(key);
        }

        @Override
        public String[] getPropertyKeys() {
            return SERVICE_KEYS.keySet().toArray(new String[]{});
        }

        @Override
        public Bundle getBundle() {
            return bundle;
        }

        @Override
        public Bundle[] getUsingBundles() {
            throw new UnsupportedOperationException("Not supported yet for RootBundleLogbackServiceReference");
        }

        @Override
        public boolean isAssignableTo(final Bundle bundle, final String className) {
            throw new UnsupportedOperationException("Not supported yet for RootBundleLogbackServiceReference");
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final RootBundleLogbackServiceReference that = (RootBundleLogbackServiceReference) o;

            return bundle != null ? bundle.equals(that.bundle) : that.bundle == null;
        }

        @Override
        public int hashCode() {
            return bundle != null ? bundle.hashCode() : 0;
        }

        @Override
        public int compareTo(final Object reference) {
            return bundle.compareTo((Bundle) reference);
        }
    }
}
