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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

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

    public TestKillbillConfigSource(@Nullable final Class<? extends PlatformDBTestingHelper> dbTestingHelperKlass) throws URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        this(null, dbTestingHelperKlass);
    }

    public TestKillbillConfigSource(@Nullable final String file, @Nullable final Class<? extends PlatformDBTestingHelper> dbTestingHelperKlass) throws IOException, URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this(file, dbTestingHelperKlass, Collections.emptyMap());
    }

    public TestKillbillConfigSource(@Nullable final String file, @Nullable final Class<? extends PlatformDBTestingHelper> dbTestingHelperKlass, final Map<String, String> extraDefaults) throws IOException, URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        super(file);

        // Set default System Properties before creating the instance of DBTestingHelper. Whereas MySQL loads its
        // driver at startup, h2 loads it statically and we need System Properties set at that point
        //populateDefaultProperties();

        if (dbTestingHelperKlass != null) {
            final PlatformDBTestingHelper dbTestingHelper = (PlatformDBTestingHelper) dbTestingHelperKlass.getDeclaredMethod("get").invoke(null);
            final EmbeddedDB instance = dbTestingHelper.getInstance();
            this.jdbcConnectionString = instance.getJdbcConnectionString();
            this.jdbcUsername = instance.getUsername();
            this.jdbcPassword = instance.getPassword();
        } else {
            // NoDB tests
            this.jdbcConnectionString = null;
            this.jdbcUsername = null;
            this.jdbcPassword = null;
        }

        this.extraDefaults = extraDefaults;
        // extraDefaults changed, need to reload defaults
       // populateDefaultProperties(extraDefaults);
        //rebuildCache();
    }

    @Override
    protected Properties getDefaultProperties() {
        final Properties properties = super.getDefaultProperties();

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

        properties.put("org.killbill.notificationq.main.sleep", "100");
        properties.put("org.killbill.notificationq.main.nbThreads", "1");
        properties.put("org.killbill.notificationq.main.claimed", "1");
        properties.put("org.killbill.notificationq.main.queue.mode", "STICKY_POLLING");
        properties.put("org.killbill.persistent.bus.main.sleep", "100");
        properties.put("org.killbill.persistent.bus.main.nbThreads", "1");
        properties.put("org.killbill.persistent.bus.main.claimed", "1");
        properties.put("org.killbill.persistent.bus.main.queue.mode", "STICKY_POLLING");
        properties.put("org.killbill.persistent.bus.external.sleep", "100");
        properties.put("org.killbill.persistent.bus.external.nbThreads", "1");
        properties.put("org.killbill.persistent.bus.external.claimed", "1");
        properties.put("org.killbill.persistent.bus.external.queue.mode", "STICKY_POLLING");
        properties.put("org.killbill.osgi.root.dir", Files.createTempDirectory().getAbsolutePath());
        properties.put("org.killbill.osgi.bundle.install.dir", Files.createTempDirectory().getAbsolutePath());

        if (extraDefaults != null) {
            for (final Entry<String, String> entry : extraDefaults.entrySet()) {
                properties.put(entry.getKey(), entry.getValue());
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