/*
 * Copyright 2010-2013 Ning, Inc.
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

package org.killbill.billing.platform.config;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.annotation.Nullable;

import org.killbill.billing.osgi.api.OSGIConfigProperties;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.xmlloader.UriAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

public class DefaultKillbillConfigSource implements KillbillConfigSource, OSGIConfigProperties {

    private static final String PROP_USER_TIME_ZONE = "user.timezone";
    private static final String PROP_SECURITY_EGD = "java.security.egd";

    private static final Logger logger = LoggerFactory.getLogger(DefaultKillbillConfigSource.class);
    private static final String PROPERTIES_FILE = "org.killbill.server.properties";
    private static final String GMT_ID = "GMT";

    private static final int NOT_SHOWN = 0;
    private static final int SHOWN = 1;

    private static volatile int GMT_WARNING = NOT_SHOWN;
    private static volatile int ENTROPY_WARNING = NOT_SHOWN;

    private final Properties properties;

    public DefaultKillbillConfigSource() throws IOException, URISyntaxException {
        this((String) null);
    }

    public DefaultKillbillConfigSource(final Map<String, String> extraDefaultProperties) throws IOException, URISyntaxException {
        this(null, extraDefaultProperties);
    }

    public DefaultKillbillConfigSource(@Nullable final String file) throws URISyntaxException, IOException {
        this(file, ImmutableMap.<String, String>of());
    }

    public DefaultKillbillConfigSource(@Nullable final String file, final Map<String, String> extraDefaultProperties) throws URISyntaxException, IOException {
        if (file == null) {
            this.properties = loadPropertiesFromFileOrSystemProperties();
        } else {
            this.properties = new Properties();
            this.properties.load(UriAccessor.accessUri(this.getClass().getResource(file).toURI()));
        }

        for (final String key : extraDefaultProperties.keySet()) {
            final String value = extraDefaultProperties.get(key);
            if (value != null) {
                properties.put(key, value);
            }
        }

        populateDefaultProperties();
    }

    @Override
    public String getString(final String propertyName) {
        return properties.getProperty(propertyName);
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    private Properties loadPropertiesFromFileOrSystemProperties() {
        // Chicken-egg problem. It would be nice to have the property in e.g. KillbillServerConfig,
        // but we need to build the ConfigSource first...
        final String propertiesFileLocation = System.getProperty(PROPERTIES_FILE);
        if (propertiesFileLocation != null) {
            try {
                // Ignore System Properties if we're loading from a file
                final Properties properties = new Properties();
                properties.load(UriAccessor.accessUri(propertiesFileLocation));
                return properties;
            } catch (final IOException e) {
                logger.warn("Unable to access properties file, defaulting to system properties", e);
            } catch (final URISyntaxException e) {
                logger.warn("Unable to access properties file, defaulting to system properties", e);
            }
        }

        return System.getProperties();
    }

    @VisibleForTesting
    protected void populateDefaultProperties() {
        final Properties defaultProperties = getDefaultProperties();
        for (final String propertyName : defaultProperties.stringPropertyNames()) {
            // Let the user override these properties
            if (properties.get(propertyName) == null) {
                properties.put(propertyName, defaultProperties.get(propertyName));
            }
        }

        final Properties defaultSystemProperties = getDefaultSystemProperties();
        for (final String propertyName : defaultSystemProperties.stringPropertyNames()) {

            // Special case to overwrite user.timezone
            if (propertyName.equals(PROP_USER_TIME_ZONE)) {
                if (!"GMT".equals(System.getProperty(propertyName))) {
                    if (GMT_WARNING == NOT_SHOWN) {
                        synchronized (DefaultKillbillConfigSource.class) {
                            if (GMT_WARNING == NOT_SHOWN) {
                                GMT_WARNING = SHOWN;
                                logger.info("Overwrite of user.timezone system property with {} may break database serialization of date. Kill Bill will overwrite to GMT",
                                            System.getProperty(propertyName));
                            }
                        }
                    }
                }

                //
                // We now set the java system property -- regardless of whether this has been set previously or not.
                // Also, setting java System property is not enough because default timezone may have been SET earlier,
                // when first call to TimeZone.getDefaultRef was invoked-- which has a side effect to set it by either looking at
                // existing "user.timezone" or being super smart by inferring from "user.country", "java.home", so we need to reset it.
                //
                System.setProperty(propertyName, GMT_ID);
                TimeZone.setDefault(TimeZone.getTimeZone(GMT_ID));
                continue;
            }

            // Let the user override these properties
            if (System.getProperty(propertyName) == null) {
                System.setProperty(propertyName, defaultSystemProperties.get(propertyName).toString());
            }
        }

        // WARN for missing PROP_SECURITY_EGD
        if (System.getProperty(PROP_SECURITY_EGD) == null) {
            if (ENTROPY_WARNING == NOT_SHOWN) {
                synchronized (DefaultKillbillConfigSource.class) {
                    if (ENTROPY_WARNING == NOT_SHOWN) {
                        ENTROPY_WARNING = SHOWN;
                        logger.warn("System property {} has not been set, this may cause some requests to hang because of a lack of entropy. You should probably set it to 'file:/dev/./urandom'", PROP_SECURITY_EGD);
                    }
                }
            }
        }
    }

    @VisibleForTesting
    public void setProperty(final String propertyName, final Object propertyValue) {
        properties.put(propertyName, propertyValue);
    }

    @VisibleForTesting
    protected Properties getDefaultProperties() {
        final Properties properties = new Properties();
        properties.put("org.killbill.persistent.bus.external.tableName", "bus_ext_events");
        properties.put("org.killbill.persistent.bus.external.historyTableName", "bus_ext_events_history");
        return properties;
    }

    @VisibleForTesting
    protected Properties getDefaultSystemProperties() {
        final Properties properties = new Properties();
        properties.put("user.timezone", GMT_ID);
        properties.put("ANTLR_USE_DIRECT_CLASS_LOADING", "true");
        // Disable log4jdbc-log4j2 by default.
        // For slf4j-simple, this doesn't quite disable it (we cannot turn off the logger completely),
        // but it should be off for logback (see logback.xml / logback-test.xml)
        properties.put("org.slf4j.simpleLogger.log.jdbc", "ERROR");
        // Sane defaults for https://code.google.com/p/log4jdbc-log4j2/
        properties.put("log4jdbc.dump.sql.maxlinelength", "0");
        properties.put("log4jdbc.spylogdelegator.name", "net.sf.log4jdbc.log.slf4j.Slf4jSpyLogDelegator");
        return properties;
    }
}
