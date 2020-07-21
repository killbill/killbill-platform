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

package org.slf4j.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.log.LogService;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class OSGISlf4jLoggerFactory implements ILoggerFactory {

    private final Map<String, OSGISlf4jLoggerAdapter> loggerMap = new HashMap<String, OSGISlf4jLoggerAdapter>();

    private LogService delegate = null;

    public Logger getLogger(final String name) {
        if (loggerMap.get(name) == null) {
            synchronized (loggerMap) {
                if (loggerMap.get(name) == null) {
                    final OSGISlf4jLoggerAdapter slf4jLogger = new OSGISlf4jLoggerAdapter(delegate);
                    loggerMap.put(name, slf4jLogger);
                }
            }
        }
        return loggerMap.get(name);
    }

    public void setDelegate(final LogService delegate) {
        synchronized (loggerMap) {
            this.delegate = delegate;
            for (final OSGISlf4jLoggerAdapter adapter : loggerMap.values()) {
                adapter.setDelegate(delegate);
            }
        }
    }
}
