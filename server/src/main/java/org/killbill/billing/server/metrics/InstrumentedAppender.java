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

package org.killbill.billing.server.metrics;

import org.killbill.commons.metrics.api.Meter;
import org.killbill.commons.metrics.api.MetricRegistry;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

// Initially forked from Dropwizard Metrics (Apache License 2.0).
// Copyright (c) 2010-2013 Coda Hale, Yammer.com, 2014-2021 Dropwizard Team
public class InstrumentedAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private final MetricRegistry registry;

    private Meter all;
    private Meter trace;
    private Meter debug;
    private Meter info;
    private Meter warn;
    private Meter error;

    /**
     * Create a new instrumented appender using the given registry.
     *
     * @param registry the metric registry
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public InstrumentedAppender(final MetricRegistry registry) {
        this.registry = registry;
        setName(Appender.class.getName());
    }

    @Override
    public void start() {
        this.all = registry.meter(getName() + ".all");
        this.trace = registry.meter(getName() + ".trace");
        this.debug = registry.meter(getName() + ".debug");
        this.info = registry.meter(getName() + ".info");
        this.warn = registry.meter(getName() + ".warn");
        this.error = registry.meter(getName() + ".error");
        super.start();
    }

    @Override
    protected void append(final ILoggingEvent event) {
        all.mark(1);
        switch (event.getLevel().toInt()) {
            case Level.TRACE_INT:
                trace.mark(1);
                break;
            case Level.DEBUG_INT:
                debug.mark(1);
                break;
            case Level.INFO_INT:
                info.mark(1);
                break;
            case Level.WARN_INT:
                warn.mark(1);
                break;
            case Level.ERROR_INT:
                error.mark(1);
                break;
            default:
                break;
        }
    }
}

