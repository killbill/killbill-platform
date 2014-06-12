/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
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

package org.killbill.billing.platform.test.config;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nullable;

import org.killbill.billing.platform.config.DefaultKillbillConfigSource;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.io.Resources;

public class TestKillbillConfigSource extends DefaultKillbillConfigSource {

    private final String jdbcConnectionString;
    private final String jdbcUsername;
    private final String jdbcPassword;
    private final Map<String, String> extraDefaults;

    public TestKillbillConfigSource() {
        this(null, null, null);
    }

    public TestKillbillConfigSource(@Nullable final String jdbcConnectionString,
                                    @Nullable final String jdbcUsername, @Nullable final String jdbcPassword) {
        super();
        this.jdbcConnectionString = jdbcConnectionString;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
        this.extraDefaults = ImmutableMap.<String, String>of();
    }

    public TestKillbillConfigSource(final String file) throws URISyntaxException, IOException {
        this(file, null, null, null, ImmutableMap.<String, String>of());
    }

    public TestKillbillConfigSource(final String file, @Nullable final String jdbcConnectionString,
                                    @Nullable final String jdbcUsername, @Nullable final String jdbcPassword) throws URISyntaxException, IOException {
        this(file, jdbcConnectionString, jdbcUsername, jdbcPassword, ImmutableMap.<String, String>of());
    }

    public TestKillbillConfigSource(final String file, @Nullable final String jdbcConnectionString,
                                    @Nullable final String jdbcUsername, @Nullable final String jdbcPassword,
                                    final Map<String, String> extraDefaults) throws URISyntaxException, IOException {
        super(file);
        this.jdbcConnectionString = jdbcConnectionString;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
        this.extraDefaults = extraDefaults;
        // extraDefaults changed, need to reload defaults
        populateDefaultProperties();
    }

    @Override
    protected Properties getDefaultProperties() {
        final Properties properties = super.getDefaultProperties();

        // Setup up DAO properties (this will be a no-op for fast tests)
        if (jdbcConnectionString != null) {
            properties.put("org.killbill.dao.url", jdbcConnectionString);
            properties.put("org.killbill.billing.osgi.dao.url", jdbcConnectionString);
        }
        if (jdbcUsername != null) {
            properties.put("org.killbill.dao.user", jdbcUsername);
            properties.put("org.killbill.billing.osgi.dao.user", jdbcUsername);
        }
        if (jdbcPassword != null) {
            properties.put("org.killbill.dao.password", jdbcPassword);
            properties.put("org.killbill.billing.osgi.dao.password", jdbcPassword);
        }

        // Speed up the notification queue
        properties.put("org.killbill.notificationq.main.sleep", "100");
        properties.put("org.killbill.notificationq.main.nbThreads", "1");
        properties.put("org.killbill.notificationq.main.prefetch", "1");
        properties.put("org.killbill.notificationq.main.claimed", "1");
        properties.put("org.killbill.notificationq.main.useInFlightQ", "false");
        // Speed up the buses
        properties.put("org.killbill.persistent.bus.main.sleep", "100");
        properties.put("org.killbill.persistent.bus.main.nbThreads", "1");
        properties.put("org.killbill.persistent.bus.main.prefetch", "1");
        properties.put("org.killbill.persistent.bus.main.claimed", "1");
        properties.put("org.killbill.persistent.bus.main.useInFlightQ", "true");
        properties.put("org.killbill.persistent.bus.external.sleep", "100");
        properties.put("org.killbill.persistent.bus.external.nbThreads", "1");
        properties.put("org.killbill.persistent.bus.external.prefetch", "1");
        properties.put("org.killbill.persistent.bus.external.claimed", "1");
        properties.put("org.killbill.persistent.bus.external.useInFlightQ", "true");

        // Temporary directory for OSGI bundles
        properties.put("org.killbill.osgi.root.dir", Files.createTempDir().getAbsolutePath());
        properties.put("org.killbill.osgi.bundle.install.dir", Files.createTempDir().getAbsolutePath());

        if (extraDefaults != null) {
            for (final String key : extraDefaults.keySet()) {
                properties.put(key, extraDefaults.get(key));
            }
        }

        return properties;
    }

    @Override
    protected Properties getDefaultSystemProperties() {
        final Properties properties = super.getDefaultSystemProperties();
        properties.put("net.sf.ehcache.skipUpdateCheck", "true");
        properties.put("org.slf4j.simpleLogger.showDateTime", "true");
        return properties;
    }
}
