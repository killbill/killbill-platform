/*
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

import java.util.UUID;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

public class LogEntryJson {

    private final UUID id;
    private final String level;
    private final String name;
    private final String message;
    private final Long time;

    public LogEntryJson(final LogEntry logEntry) {
        id = UUID.randomUUID();

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

        message = logEntry.getMessage();
        time = logEntry.getTime();
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
        final StringBuffer sb = new StringBuffer("LogEntryJson{");
        sb.append("id=").append(id);
        sb.append(", level='").append(level).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", time=").append(time);
        sb.append('}');
        return sb.toString();
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
