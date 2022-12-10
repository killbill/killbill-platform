/*
 * Copyright 2020-2022 Equinix, Inc
 * Copyright 2014-2022 The Billing Project, LLC
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
import java.util.Objects;

import org.killbill.commons.utils.annotation.VisibleForTesting;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

public class KillbillLoggerFactory implements LoggerFactory {

    private final Bundle bundle;
    private final Map<LoggersKey, Logger> loggers;

    public KillbillLoggerFactory(final Bundle bundle) {
        this.bundle = bundle;
        this.loggers = new HashMap<>();
    }

    @Override
    public Logger getLogger(final String name) {
        return getLogger(name, KillbillLogger.class);
    }

    @Override
    public Logger getLogger(final Class<?> clazz) {
        return getLogger(clazz, KillbillLogger.class);
    }

    @Override
    public <L extends Logger> L getLogger(final String name, final Class<L> loggerType) {
        return getLogger(bundle, name, loggerType);
    }

    @Override
    public <L extends Logger> L getLogger(final Class<?> clazz, final Class<L> loggerType) {
        return getLogger(bundle, clazz.getName(), loggerType);
    }

    @Override
    public <L extends Logger> L getLogger(final Bundle bundle, final String name, final Class<L> loggerType) {
        final LoggersKey key = new LoggersKey(bundle, name);
        final Logger logger = loggers.get(key);
        if (logger != null) {
            return (L) logger;
        }
        synchronized (loggers) {
            final Logger newLogger = new KillbillLogger(key.getLoggerName());
            loggers.put(key, newLogger);
            return (L) newLogger;
        }
    }

    public Logger getLogger() {
        return getLogger((String) null);
    }

    @VisibleForTesting
    int getLoggersSize() {
        return loggers.size();
    }

    static class LoggersKey {

        private final String loggerName;

        public LoggersKey(final Bundle bundle) {
            this(bundle, null);
        }

        public LoggersKey(final Bundle bundle, final String className) {
            this.loggerName = createLoggerName(bundle, className);
        }

        public String getLoggerName() {
            return loggerName;
        }

        @VisibleForTesting
        static String createLoggerName(final Bundle bundle, String className) {
            className = Objects.requireNonNullElse(className, "");
            if (!className.isEmpty() && !className.isBlank()) {
                return className;
            }

            // As per specs: https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.log.html#d0e2377
            // "Normally the name of the class which is doing the logging is used as the logger name"
            // But KillbillLogWriter need to support (obsolete) LogService where there's no way to access logger's class
            // client. So we need logger name alternative: bundle+version as its name, or if not possible,
            // use KillbillLogWriter as logger name.
            final String bundleName = createBundleName(bundle);
            if (!bundleName.isEmpty() && !bundleName.isBlank()) {
                return bundleName;
            }

            return KillbillLogWriter.class.getName();
        }

        @VisibleForTesting
        static String createBundleName(final Bundle bundle) {
            if (bundle == null || bundle.getSymbolicName() == null || bundle.getSymbolicName().isBlank()) {
                return "";
            }
            final String version = bundle.getVersion() == null ||
                                   bundle.getVersion().toString() == null ||
                                   bundle.getVersion().toString().isBlank()
                                   ? Version.emptyVersion.toString() : bundle.getVersion().toString();
            return bundle.getSymbolicName() + ":" + version.replace(".", "_");
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final LoggersKey that = (LoggersKey) o;
            return loggerName.equals(that.loggerName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(loggerName);
        }

        @Override
        public String toString() {
            return "LoggersKey{ loggerName='" + loggerName + "' }";
        }
    }
}
