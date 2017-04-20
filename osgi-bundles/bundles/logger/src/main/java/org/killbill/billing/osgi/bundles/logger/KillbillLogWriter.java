/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
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
import java.util.Queue;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.collect.EvictingQueue;

// Inspired by osgi-over-slf4j
public class KillbillLogWriter implements LogListener {

    private static final String UNKNOWN = "[Unknown]";
    private static final String OSGI_BUNDLES_JRUBY = "org.kill-bill.billing.killbill-platform-osgi-bundles-jruby";
    private static final String MDC_KEY = "MDC";

    private final Map<String, Logger> delegates = new HashMap<String, Logger>();
    private final Queue<LogEntry> latestEntries = EvictingQueue.<LogEntry>create(500);

    // Invoked by the log service implementation for each log entry
    @SuppressWarnings("unchecked")
    public void logged(final LogEntry entry) {
        final Bundle bundle = entry.getBundle();
        final Logger delegate = getDelegateForBundle(bundle);

        final ServiceReference serviceReference = entry.getServiceReference();
        final int level = entry.getLevel();
        final String message = entry.getMessage();
        final Throwable exception = entry.getException();

        if (serviceReference != null) {
            // A single thread (e.g. org.apache.felix.log.LogListenerThread) should be invoking this, but just to be safe...
            synchronized (this) {
                try {
                    final Object originalMdcMap = serviceReference.getProperty(MDC_KEY);
                    if (originalMdcMap != null) {
                        MDC.setContextMap((Map) originalMdcMap);
                    }

                    if (exception != null) {
                        log(delegate, serviceReference, level, message, exception);
                    } else {
                        log(delegate, serviceReference, level, message);
                    }
                } finally {
                    MDC.clear();
                }
            }
        } else if (exception != null) {
            log(delegate, level, message, exception);
        } else {
            log(delegate, level, message);
        }

        latestEntries.offer(entry);
    }

    public Queue<LogEntry> getLatestEntries() {
        return latestEntries;
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

    private void log(final Logger delegate, final int level, final String message) {
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

    private void log(final Logger delegate, final int level, final String message, final Throwable exception) {
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

    private void log(final Logger delegate, final ServiceReference sr, final int level, final String message) {
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

    private void log(final Logger delegate, final ServiceReference sr, final int level, final String message, final Throwable exception) {
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
}
