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

import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerConsumer;

class KillbillLogger implements Logger {

    private final org.slf4j.Logger slf4jLogger;

    public KillbillLogger(final String name) {
        slf4jLogger = org.slf4j.LoggerFactory.getLogger(name);
    }

    @Override
    public String getName() {
        return slf4jLogger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return slf4jLogger.isTraceEnabled();
    }

    @Override
    public void trace(final String message) {
        slf4jLogger.trace(message);
    }

    @Override
    public void trace(final String format, final Object arg) {
        slf4jLogger.trace(format, arg);
    }

    @Override
    public void trace(final String format, final Object arg1, final Object arg2) {
        slf4jLogger.trace(format, arg1, arg2);
    }

    @Override
    public void trace(final String format, final Object... arguments) {
        slf4jLogger.trace(format, arguments);
    }

    @Override
    public <E extends Exception> void trace(final LoggerConsumer<E> consumer) throws E {
        consumer.accept(this);
    }

    @Override
    public boolean isDebugEnabled() {
        return slf4jLogger.isDebugEnabled();
    }

    @Override
    public void debug(final String message) {
        slf4jLogger.debug(message);
    }

    @Override
    public void debug(final String format, final Object arg) {
        slf4jLogger.debug(format, arg);
    }

    @Override
    public void debug(final String format, final Object arg1, final Object arg2) {
        slf4jLogger.debug(format, arg1, arg2);
    }

    @Override
    public void debug(final String format, final Object... arguments) {
        slf4jLogger.debug(format, arguments);
    }

    @Override
    public <E extends Exception> void debug(final LoggerConsumer<E> consumer) throws E {
        consumer.accept(this);
    }

    @Override
    public boolean isInfoEnabled() {
        return slf4jLogger.isInfoEnabled();
    }

    @Override
    public void info(final String message) {
        slf4jLogger.info(message);
    }

    @Override
    public void info(final String format, final Object arg) {
        slf4jLogger.info(format, arg);
    }

    @Override
    public void info(final String format, final Object arg1, final Object arg2) {
        slf4jLogger.info(format, arg1, arg2);
    }

    @Override
    public void info(final String format, final Object... arguments) {
        slf4jLogger.info(format, arguments);
    }

    @Override
    public <E extends Exception> void info(final LoggerConsumer<E> consumer) throws E {
        consumer.accept(this);
    }

    @Override
    public boolean isWarnEnabled() {
        return slf4jLogger.isWarnEnabled();
    }

    @Override
    public void warn(final String message) {
        slf4jLogger.warn(message);
    }

    @Override
    public void warn(final String format, final Object arg) {
        slf4jLogger.warn(format, arg);
    }

    @Override
    public void warn(final String format, final Object arg1, final Object arg2) {
        slf4jLogger.warn(format, arg1, arg2);
    }

    @Override
    public void warn(final String format, final Object... arguments) {
        slf4jLogger.warn(format, arguments);
    }

    @Override
    public <E extends Exception> void warn(final LoggerConsumer<E> consumer) throws E {
        consumer.accept(this);
    }

    @Override
    public boolean isErrorEnabled() {
        return slf4jLogger.isErrorEnabled();
    }

    @Override
    public void error(final String message) {
        slf4jLogger.error(message);
    }

    @Override
    public void error(final String format, final Object arg) {
        slf4jLogger.error(format, arg);
    }

    @Override
    public void error(final String format, final Object arg1, final Object arg2) {
        slf4jLogger.error(format, arg1, arg2);
    }

    @Override
    public void error(final String format, final Object... arguments) {
        slf4jLogger.error(format, arguments);
    }

    @Override
    public <E extends Exception> void error(final LoggerConsumer<E> consumer) throws E {
        consumer.accept(this);
    }

    @Override
    public void audit(final String message) {
        slf4jLogger.info(message);
    }

    @Override
    public void audit(final String format, final Object arg) {
        slf4jLogger.info(format, arg);
    }

    @Override
    public void audit(final String format, final Object arg1, final Object arg2) {
        slf4jLogger.info(format, arg1, arg2);
    }

    @Override
    public void audit(final String format, final Object... arguments) {
        slf4jLogger.info(format, arguments);
    }
}
