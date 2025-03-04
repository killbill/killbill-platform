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

package org.killbill.billing.osgi.bundles.logger;

import java.util.UUID;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LogEntryJson {

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    private final UUID id;
    private final String level;
    private final String name;
    private final String logger;
    private final String message;
    private final Long time;

    public LogEntryJson(final Bundle bundle, final int intLevel, final String loggerName, final String message, final Throwable exception) {
        id = UUID.randomUUID();

        if (intLevel == LogService.LOG_ERROR) {
            level = "ERROR";
        } else if (intLevel == LogService.LOG_WARNING) {
            level = "WARNING";
        } else if (intLevel == LogService.LOG_INFO) {
            level = "INFO";
        } else if (intLevel == LogService.LOG_DEBUG) {
            level = "DEBUG";
        } else {
            level = String.valueOf(intLevel);
        }

        if (bundle == null) {
            name = null;
        } else if (bundle.getLocation() != null && bundle.getLocation().startsWith("jruby-")) {
            // Extract the plugin name (see FileInstall)
            name = bundle.getLocation().substring(6);
        } else if (bundle.getSymbolicName() != null && bundle.getSymbolicName().startsWith("org.kill-bill.billing.plugin.java")) {
            // Extract the plugin name
            name = bundle.getSymbolicName().substring(34);
        } else {
            name = bundle.getSymbolicName();
        }

        this.logger = loggerName;
        this.message = message;
        this.time = System.currentTimeMillis();
    }

    public String getLogger() {
        return logger;
    }

    public UUID getId() {
        return id;
    }

    public String getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public Long getTime() {
        return time;
    }

    @Override
    public String toString() {
        try {
            // Always return JSON in case this method is used as a last resort serialization mechanism
            return DEFAULT_OBJECT_MAPPER.writeValueAsString(this);
        } catch (final JsonProcessingException e) {
            final StringBuffer sb = new StringBuffer("{");
            sb.append("\"id\":\"").append(id).append("\"");
            sb.append(", \"level\":\"").append(level).append("\"");
            sb.append(", \"name\":\"").append(name).append("\"");
            sb.append(", \"logger\":\"").append(logger).append("\"");
            sb.append(", \"message\":\"").append(message).append("\"");
            sb.append(", \"time\":\"").append(time).append("\"");
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LogEntryJson that = (LogEntryJson) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (level != null ? !level.equals(that.level) : that.level != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (message != null ? !message.equals(that.message) : that.message != null) {
            return false;
        }
        return time != null ? time.equals(that.time) : that.time == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (level != null ? level.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        return result;
    }
}
