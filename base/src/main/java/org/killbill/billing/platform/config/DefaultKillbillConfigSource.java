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

import javax.annotation.Nullable;

import org.killbill.billing.osgi.api.OSGIConfigProperties;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.xmlloader.UriAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

public class DefaultKillbillConfigSource implements KillbillConfigSource, OSGIConfigProperties {

    private static final Logger logger = LoggerFactory.getLogger(DefaultKillbillConfigSource.class);
    private static final String PROPERTIES_FILE = "org.killbill.server.properties";

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
            // Let the user override these properties
            if (System.getProperty(propertyName) == null) {
                System.setProperty(propertyName, defaultSystemProperties.get(propertyName).toString());
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
        properties.put("user.timezone", "UTC");
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
