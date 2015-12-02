/*
 * Copyright 2015 Groupon, Inc
 * Copyright 2015 The Billing Project, LLC
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class LogsServlet extends HttpServlet {

    private static final JsonFactory factory = new JsonFactory();

    private final KillbillLogWriter logWriter;

    public LogsServlet(final KillbillLogWriter logWriter) {
        this.logWriter = logWriter;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final JsonGenerator jg = factory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8);

        try {
            jg.writeStartArray();
            for (final LogEntry logEntry : logWriter.getLatestEntries()) {
                jg.writeStartObject();

                final String level;
                if (logEntry.getLevel() == LogService.LOG_ERROR) {
                    level = "ERROR";
                } else if (logEntry.getLevel() == LogService.LOG_WARNING) {
                    level = "WARNING";
                } else if (logEntry.getLevel() == LogService.LOG_INFO) {
                    level = "INFO";
                } else if (logEntry.getLevel() == LogService.LOG_DEBUG) {
                    level = "DEBUG";
                } else {
                    level = String.valueOf(logEntry.getLevel());
                }
                jg.writeStringField("level", level);

                final String name;
                if (logEntry.getBundle() == null) {
                    name = null;
                } else if (logEntry.getBundle().getLocation() != null && logEntry.getBundle().getLocation().startsWith("jruby-")) {
                    // Extract the plugin name (see FileInstall)
                    name = logEntry.getBundle().getLocation().substring(6);
                } else if (logEntry.getBundle().getSymbolicName() != null && logEntry.getBundle().getSymbolicName().startsWith("org.kill-bill.billing.plugin.java")) {
                    // Extract the plugin name
                    name = logEntry.getBundle().getSymbolicName().substring(34);
                } else {
                    name = logEntry.getBundle().getSymbolicName();
                }
                jg.writeStringField("name", name);

                jg.writeStringField("message", logEntry.getMessage());
                jg.writeNumberField("time", logEntry.getTime());

                jg.writeEndObject();
            }
            jg.writeEndArray();
        } finally {
            jg.close();
        }
    }
}
