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

package org.killbill.billing.platform.test.config;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.annotation.Nullable;

import org.killbill.billing.platform.config.DefaultKillbillConfigSource;
import org.killbill.billing.platform.test.PlatformDBTestingHelper;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.utils.io.Files;

public class TestKillbillConfigSource extends DefaultKillbillConfigSource {

    private final String jdbcConnectionString;
    private final String jdbcUsername;
    private final String jdbcPassword;
    private final Map<String, String> extraDefaults;

    // Flag to control whether we should skip timezone setting during initialization
    private static final ThreadLocal<Boolean> SKIP_TIMEZONE_INIT = ThreadLocal.withInitial(() -> false);

    public TestKillbillConfigSource(@Nullable final Class<? extends PlatformDBTestingHelper> dbTestingHelperKlass) throws URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        this(null, dbTestingHelperKlass);
    }

    public TestKillbillConfigSource(@Nullable final String file, @Nullable final Class<? extends PlatformDBTestingHelper> dbTestingHelperKlass) throws IOException, URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this(file, dbTestingHelperKlass, Collections.emptyMap());
    }

    public TestKillbillConfigSource(@Nullable final String file, @Nullable final Class<? extends PlatformDBTestingHelper> dbTestingHelperKlass, final Map<String, String> extraDefaults) throws IOException, URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        super(file, initializeAndBuildDefaults(dbTestingHelperKlass, extraDefaults));

        // Store references for potential later use
        if (dbTestingHelperKlass != null) {
            final PlatformDBTestingHelper dbTestingHelper = (PlatformDBTestingHelper) dbTestingHelperKlass.getDeclaredMethod("get").invoke(null);
            final EmbeddedDB instance = dbTestingHelper.getInstance();
            this.jdbcConnectionString = instance.getJdbcConnectionString();
            this.jdbcUsername = instance.getUsername();
            this.jdbcPassword = instance.getPassword();
        } else {
            this.jdbcConnectionString = null;
            this.jdbcUsername = null;
            this.jdbcPassword = null;
        }

        this.extraDefaults = extraDefaults;

        // Clean up the flag
        SKIP_TIMEZONE_INIT.remove();

        // NOW apply timezone setting after DB is fully initialized
        System.setProperty("user.timezone", "GMT");
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    private static Map<String, String> initializeAndBuildDefaults(@Nullable final Class<? extends PlatformDBTestingHelper> dbTestingHelperKlass, final Map<String, String> extraDefaults) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Mark that we want to skip timezone init temporarily
        SKIP_TIMEZONE_INIT.set(true);

        return buildExtraDefaults(dbTestingHelperKlass, extraDefaults);
    }

    private static Map<String, String> buildExtraDefaults(@Nullable final Class<? extends PlatformDBTestingHelper> dbTestingHelperKlass, final Map<String, String> extraDefaults) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Map<String, String> allDefaults = new HashMap<>(extraDefaults);

        // Initialize DB and add JDBC properties
        if (dbTestingHelperKlass != null) {
            final PlatformDBTestingHelper dbTestingHelper = (PlatformDBTestingHelper) dbTestingHelperKlass.getDeclaredMethod("get").invoke(null);
            final EmbeddedDB instance = dbTestingHelper.getInstance();

            allDefaults.put("org.killbill.dao.url", instance.getJdbcConnectionString());
            allDefaults.put("org.killbill.billing.osgi.dao.url", instance.getJdbcConnectionString());
            allDefaults.put("org.killbill.dao.user", instance.getUsername());
            allDefaults.put("org.killbill.billing.osgi.dao.user", instance.getUsername());
            allDefaults.put("org.killbill.dao.password", instance.getPassword());
            allDefaults.put("org.killbill.billing.osgi.dao.password", instance.getPassword());
        }

        // Add test-specific properties
        allDefaults.put("org.killbill.notificationq.main.sleep", "100");
        allDefaults.put("org.killbill.notificationq.main.nbThreads", "1");
        allDefaults.put("org.killbill.notificationq.main.claimed", "1");
        allDefaults.put("org.killbill.notificationq.main.queue.mode", "STICKY_POLLING");
        allDefaults.put("org.killbill.persistent.bus.main.sleep", "100");
        allDefaults.put("org.killbill.persistent.bus.main.nbThreads", "1");
        allDefaults.put("org.killbill.persistent.bus.main.claimed", "1");
        allDefaults.put("org.killbill.persistent.bus.main.queue.mode", "STICKY_POLLING");
        allDefaults.put("org.killbill.persistent.bus.external.sleep", "100");
        allDefaults.put("org.killbill.persistent.bus.external.nbThreads", "1");
        allDefaults.put("org.killbill.persistent.bus.external.claimed", "1");
        allDefaults.put("org.killbill.persistent.bus.external.queue.mode", "STICKY_POLLING");

        allDefaults.put("org.killbill.osgi.root.dir", Files.createTempDirectory().getAbsolutePath());
        allDefaults.put("org.killbill.osgi.bundle.install.dir", Files.createTempDirectory().getAbsolutePath());

        return allDefaults;
    }

    @Override
    protected Properties getDefaultSystemProperties() {
        final Properties properties = super.getDefaultSystemProperties();

        // Skip timezone if we're in the initialization phase
        if (SKIP_TIMEZONE_INIT.get()) {
            properties.remove("user.timezone");
        }

        properties.put("net.sf.ehcache.skipUpdateCheck", "true");
        properties.put("org.slf4j.simpleLogger.showDateTime", "true");
        return properties;
    }
}