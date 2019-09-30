/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014-2019 Groupon, Inc
 * Copyright 2014-2019 The Billing Project, LLC
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

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

// Kill Bill LogService implementation: forwards OSGI logs to slf4j
// Note: no LogReaderService implementation is currently provided (LogListener implementations would never be called)
// Plugins using LoggerFactory.getLogger directly would not use this (redirected to logback by default)
public class KillbillLogWriter implements BundleListener, FrameworkListener, ServiceListener, LogService {

    private static final String UNKNOWN = "[Unknown]";
    private static final String OSGI_BUNDLES_JRUBY = "org.kill-bill.billing.killbill-platform-osgi-bundles-jruby";
    private static final String MDC_KEY = "MDC";

    private final Map<String, Logger> delegates = new HashMap<String, Logger>();
    private final LogEntriesManager logEntriesManager;

    public KillbillLogWriter(final LogEntriesManager logEntriesManager) {
        this.logEntriesManager = logEntriesManager;
    }

    @Override
    public void log(final ServiceReference serviceReference, final int level, final String message, final Throwable exception) {
        final Bundle bundle = serviceReference == null ? null : serviceReference.getBundle();

        // Forward the log to HTTP consumers
        logEntriesManager.recordEvent(new LogEntryJson(bundle, level, message, exception));

        if (serviceReference != null && "true".equals(serviceReference.getProperty("KILL_BILL_ROOT_LOGGING"))) {
            // LogEntry comes from Logback already (see OSGIAppender), ignore
            return;
        }

        // Log comes from a pure OSGI LogService, forward it to slf4j
        final Logger delegate = getDelegateForBundle(bundle);
        if (serviceReference != null) {
            // A single thread (e.g. org.apache.felix.log.LogListenerThread) should be invoking this, but just to be safe...
            synchronized (this) {
                try {
                    final Object originalMdcMap = serviceReference.getProperty(MDC_KEY);
                    if (originalMdcMap != null) {
                        //noinspection unchecked
                        MDC.setContextMap((Map) originalMdcMap);
                    }

                    if (exception != null) {
                        logInternal(delegate, serviceReference, level, message, exception);
                    } else {
                        logInternal(delegate, serviceReference, level, message);
                    }
                } finally {
                    MDC.clear();
                }
            }
        } else if (exception != null) {
            logInternal(delegate, level, message, exception);
        } else {
            logInternal(delegate, level, message);
        }
    }

    private Logger getDelegateForBundle(/* @Nullable */ final Bundle bundle) {
        final String loggerName;
        if (bundle != null) {
            String name = bundle.getSymbolicName();
            Version version = bundle.getVersion();
            if (version == null) {
                version = Version.emptyVersion;
            }

            // Prettier name for Ruby plugins
            if (name.startsWith(OSGI_BUNDLES_JRUBY)) {
                name = "jruby";
            }

            // Don't use . as a separator (to avoid any truncation by the logging system)
            loggerName = name + ':' + version.toString().replace(".", "_");
        } else {
            loggerName = KillbillLogWriter.class.getName();
        }

        if (delegates.get(loggerName) == null) {
            synchronized (delegates) {
                if (delegates.get(loggerName) == null) {
                    delegates.put(loggerName, LoggerFactory.getLogger(loggerName));
                }
            }
        }

        return delegates.get(loggerName);
    }

    private void logInternal(final Logger delegate, final int level, final String message) {
        switch (level) {
            case LogService.LOG_DEBUG:
                delegate.debug(message);
                break;
            case LogService.LOG_ERROR:
                delegate.error(message);
                break;
            case LogService.LOG_INFO:
                delegate.info(message);
                break;
            case LogService.LOG_WARNING:
                delegate.warn(message);
                break;
            default:
                break;
        }
    }

    private void logInternal(final Logger delegate, final int level, final String message, final Throwable exception) {
        switch (level) {
            case LogService.LOG_DEBUG:
                delegate.debug(message, exception);
                break;
            case LogService.LOG_ERROR:
                delegate.error(message, exception);
                break;
            case LogService.LOG_INFO:
                delegate.info(message, exception);
                break;
            case LogService.LOG_WARNING:
                delegate.warn(message, exception);
                break;
            default:
                break;
        }
    }

    private void logInternal(final Logger delegate, final ServiceReference sr, final int level, final String message) {
        switch (level) {
            case LogService.LOG_DEBUG:
                if (delegate.isDebugEnabled()) {
                    delegate.debug(createMessage(sr, message));
                }
                break;
            case LogService.LOG_ERROR:
                if (delegate.isErrorEnabled()) {
                    delegate.error(createMessage(sr, message));
                }
                break;
            case LogService.LOG_INFO:
                if (delegate.isInfoEnabled()) {
                    delegate.info(createMessage(sr, message));
                }
                break;
            case LogService.LOG_WARNING:
                if (delegate.isWarnEnabled()) {
                    delegate.warn(createMessage(sr, message));
                }
                break;
            default:
                break;
        }
    }

    private void logInternal(final Logger delegate, final ServiceReference sr, final int level, final String message, final Throwable exception) {
        switch (level) {
            case LogService.LOG_DEBUG:
                if (delegate.isDebugEnabled()) {
                    delegate.debug(createMessage(sr, message), exception);
                }
                break;
            case LogService.LOG_ERROR:
                if (delegate.isErrorEnabled()) {
                    delegate.error(createMessage(sr, message), exception);
                }
                break;
            case LogService.LOG_INFO:
                if (delegate.isInfoEnabled()) {
                    delegate.info(createMessage(sr, message), exception);
                }
                break;
            case LogService.LOG_WARNING:
                if (delegate.isWarnEnabled()) {
                    delegate.warn(createMessage(sr, message), exception);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Formats the log message to indicate the service sending it, if known.
     *
     * @param sr      the ServiceReference sending the message.
     * @param message The message to log.
     * @return The formatted log message.
     */
    private String createMessage(final ServiceReference sr, final String message) {
        final StringBuilder output = new StringBuilder();

        final String prefix;
        if (sr != null) {
            if ("org.killbill.killbill.osgi.libs.killbill.OSGIKillbillServiceReference".equals(sr.getClass().getName())) {
                // Not interesting (OSGIKillbillLogService wrapper)
                prefix = null;
            } else {
                final String srObjectClass = sr.toString();
                if (srObjectClass != null && srObjectClass.startsWith("[")) {
                    // Felix already appends these, see ServiceRegistrationImpl
                    prefix = srObjectClass;
                } else {
                    prefix = "[" + srObjectClass + "]";
                }
            }
        } else {
            prefix = UNKNOWN;
        }

        if (prefix != null) {
            output.append(prefix).append(' ');
        }
        output.append(message);

        return output.toString();
    }

    private static final String[] BUNDLE_EVENT_MESSAGES =
            {
                    "[%s] BundleEvent INSTALLED",
                    "[%s] BundleEvent STARTED",
                    "[%s] BundleEvent STOPPED",
                    "[%s] BundleEvent UPDATED",
                    "[%s] BundleEvent UNINSTALLED",
                    "[%s] BundleEvent RESOLVED",
                    "[%s] BundleEvent UNRESOLVED"
            };

    @Override
    public void bundleChanged(final BundleEvent event) {
        final int eventType = event.getType();
        String message = null;

        for (int i = 0; message == null && i < BUNDLE_EVENT_MESSAGES.length; ++i) {
            if (eventType >> i == 1) {
                message = BUNDLE_EVENT_MESSAGES[i];
            }
        }

        if (message != null) {
            log(LogService.LOG_INFO, String.format(message, event.getBundle() == null ? "?" : event.getBundle().getSymbolicName()));
        }
    }

    private static final String[] FRAMEWORK_EVENT_MESSAGES =
            {
                    "[%s] FrameworkEvent STARTED",
                    "[%s] FrameworkEvent ERROR",
                    "[%s] FrameworkEvent PACKAGES REFRESHED",
                    "[%s] FrameworkEvent STARTLEVEL CHANGED",
                    "[%s] FrameworkEvent WARNING",
                    "[%s] FrameworkEvent INFO"
            };

    @Override
    public void frameworkEvent(final FrameworkEvent event) {
        final int eventType = event.getType();
        String message = null;

        for (int i = 0; message == null && i < FRAMEWORK_EVENT_MESSAGES.length; ++i) {
            if (eventType >> i == 1) {
                message = FRAMEWORK_EVENT_MESSAGES[i];
            }
        }

        if (message != null) {
            log((eventType == FrameworkEvent.ERROR) ? LogService.LOG_ERROR : LogService.LOG_INFO,
                String.format(message, event.getBundle() == null ? "?" : event.getBundle().getSymbolicName()),
                event.getThrowable());
        }
    }

    private static final String[] SERVICE_EVENT_MESSAGES =
            {
                    "[%s] ServiceEvent REGISTERED",
                    "[%s] ServiceEvent MODIFIED",
                    "[%s] ServiceEvent UNREGISTERING"
            };

    @Override
    public void serviceChanged(final ServiceEvent event) {
        final int eventType = event.getType();
        String message = null;

        for (int i = 0; message == null && i < SERVICE_EVENT_MESSAGES.length; ++i) {
            if (eventType >> i == 1) {
                message = SERVICE_EVENT_MESSAGES[i];
            }
        }

        if (message != null) {
            log((eventType == ServiceEvent.MODIFIED) ? LogService.LOG_DEBUG : LogService.LOG_INFO,
                String.format(message, (event.getServiceReference() == null || event.getServiceReference().getBundle() == null) ? "?" : event.getServiceReference().getBundle().getSymbolicName()));
        }
    }

    @Override
    public void log(final int level, final String message) {
        log(null, level, message, null);
    }

    @Override
    public void log(final int level, final String message, final Throwable exception) {
        log(null, level, message, exception);
    }

    @Override
    public void log(final ServiceReference sr, final int level, final String message) {
        log(sr, level, message, null);
    }
}
