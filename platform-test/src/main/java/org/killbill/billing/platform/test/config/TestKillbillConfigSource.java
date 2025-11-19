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

    public TestKillbillConfigSource(@Nullable final Class<? extends PlatformDBTestingHelper> dbTestingHelperKlass) throws Exception {
        this(null, dbTestingHelperKlass);
    }

    public TestKillbillConfigSource(@Nullable final String file, @Nullable final Class<? extends PlatformDBTestingHelper> dbTestingHelperKlass) throws Exception {
        this(file, dbTestingHelperKlass, Collections.emptyMap());
    }

    public TestKillbillConfigSource(@Nullable final String file, @Nullable final Class<? extends PlatformDBTestingHelper> dbTestingHelperKlass, final Map<String, String> extraDefaults) throws Exception {
        super(file, buildPropertiesMap(dbTestingHelperKlass, extraDefaults));

        populateDefaultProperties(extraDefaults);

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

        populateDefaultProperties(extraDefaults);

        rebuildCache();

        System.out.println("Output of getPropertiesBySource....");
        getPropertiesBySource().forEach((s, stringStringMap) -> {
            System.out.println(s);
            stringStringMap.forEach((s1, s2) -> System.out.println("  " + s1 + ": " + s2));
        });
    }

    private static Map<String, String> buildPropertiesMap(Class<? extends PlatformDBTestingHelper> dbTestingHelperKlass, Map<String, String> extraDefaults) throws Exception {
        Map<String, String> props = new HashMap<>();


        if (dbTestingHelperKlass != null) {
            final PlatformDBTestingHelper dbTestingHelper = (PlatformDBTestingHelper) dbTestingHelperKlass.getDeclaredMethod("get").invoke(null);
            final EmbeddedDB instance = dbTestingHelper.getInstance();

            String jdbcUrl = instance.getJdbcConnectionString();
            String user = instance.getUsername();
            String password = instance.getPassword();

            if (jdbcUrl != null) {
                props.put("org.killbill.dao.url", jdbcUrl);
                props.put("org.killbill.billing.osgi.dao.url", jdbcUrl);

                if (jdbcUrl.contains(":h2:")) {
                    props.put("org.killbill.dao.driverClassName", "org.h2.Driver");
                    props.put("org.killbill.billing.osgi.dao.driverClassName", "org.h2.Driver");
                } else if (jdbcUrl.contains(":postgresql:")) {
                    props.put("org.killbill.dao.driverClassName", "org.postgresql.Driver");
                    props.put("org.killbill.billing.osgi.dao.driverClassName", "org.postgresql.Driver");
                } else if (jdbcUrl.contains(":mysql:")) {
                    props.put("org.killbill.dao.driverClassName", "com.mysql.cj.jdbc.Driver");
                    props.put("org.killbill.billing.osgi.dao.driverClassName", "com.mysql.cj.jdbc.Driver");
                }
            }
            if (user != null) {
                props.put("org.killbill.dao.user", user);
                props.put("org.killbill.billing.osgi.dao.user", user);
            }
            if (password != null) {
                props.put("org.killbill.dao.password", password);
                props.put("org.killbill.billing.osgi.dao.password", password);
            }
        }

        props.put("org.killbill.notificationq.main.sleep", "100");
        props.put("org.killbill.notificationq.main.nbThreads", "1");
        props.put("org.killbill.notificationq.main.claimed", "1");
        props.put("org.killbill.notificationq.main.queue.mode", "STICKY_POLLING");

        props.put("org.killbill.persistent.bus.main.sleep", "100");
        props.put("org.killbill.persistent.bus.main.nbThreads", "1");
        props.put("org.killbill.persistent.bus.main.claimed", "1");
        props.put("org.killbill.persistent.bus.main.queue.mode", "STICKY_POLLING");

        props.put("org.killbill.persistent.bus.external.sleep", "100");
        props.put("org.killbill.persistent.bus.external.nbThreads", "1");
        props.put("org.killbill.persistent.bus.external.claimed", "1");
        props.put("org.killbill.persistent.bus.external.queue.mode", "STICKY_POLLING");

        props.put("org.killbill.osgi.root.dir", Files.createTempDirectory().getAbsolutePath());
        props.put("org.killbill.osgi.bundle.install.dir", Files.createTempDirectory().getAbsolutePath());

        if (extraDefaults != null) {
            props.putAll(extraDefaults);
        }

        return props;
    }

    @Override
    protected Properties getDefaultSystemProperties() {
        final Properties properties = super.getDefaultSystemProperties();
        properties.put("net.sf.ehcache.skipUpdateCheck", "true");
        properties.put("org.slf4j.simpleLogger.showDateTime", "true");
        return properties;
    }
}