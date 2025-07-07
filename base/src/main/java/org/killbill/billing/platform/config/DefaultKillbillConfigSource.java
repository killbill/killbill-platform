/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2021 Equinix, Inc
 * Copyright 2014-2021 The Billing Project, LLC
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;

import javax.annotation.Nullable;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.killbill.billing.osgi.api.OSGIConfigProperties;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.commons.utils.Strings;
import org.killbill.commons.utils.annotation.VisibleForTesting;
import org.killbill.xmlloader.UriAccessor;
import org.skife.config.RuntimeConfigRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultKillbillConfigSource implements KillbillConfigSource, OSGIConfigProperties {

    private static final Object lock = new Object();

    private static final String PROP_USER_TIME_ZONE = "user.timezone";
    private static final String PROP_SECURITY_EGD = "java.security.egd";

    private static final Logger logger = LoggerFactory.getLogger(DefaultKillbillConfigSource.class);
    private static final String PROPERTIES_FILE = "org.killbill.server.properties";
    private static final String GMT_ID = "GMT";

    private static final String LOOKUP_ENVIRONMENT_VARIABLES = "org.killbill.server.lookupEnvironmentVariables";
    static final String ENVIRONMENT_VARIABLE_PREFIX = "KB_";

    private static final String ENABLE_JASYPT_DECRYPTION = "org.killbill.server.enableJasypt";
    private static final String JASYPT_ENCRYPTOR_PASSWORD_KEY = "JASYPT_ENCRYPTOR_PASSWORD";
    private static final String JASYPT_ENCRYPTOR_ALGORITHM_KEY = "JASYPT_ENCRYPTOR_ALGORITHM";
    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";

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
        this(file, Collections.emptyMap());
    }

    public DefaultKillbillConfigSource(@Nullable final String file, final Map<String, String> extraDefaultProperties) throws URISyntaxException, IOException {
        if (file == null) {
            this.properties = loadPropertiesFromFileOrSystemProperties();
        } else {
            this.properties = new Properties();
            this.properties.load(UriAccessor.accessUri(Objects.requireNonNull(this.getClass().getResource(file)).toURI()));
        }

        for (final Entry<String, String> entry : extraDefaultProperties.entrySet()) {
            if (entry.getValue() != null) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }

        populateDefaultProperties();

        if (Boolean.parseBoolean(getString(LOOKUP_ENVIRONMENT_VARIABLES))) {
            overrideWithEnvironmentVariables();
        }

        if (Boolean.parseBoolean(getString(ENABLE_JASYPT_DECRYPTION))) {
            decryptJasyptProperties();
        }
    }

    @Override
    public String getString(final String propertyName) {
        return properties.getProperty(propertyName);
    }

    @Override
    public Properties getProperties() {
        final Properties result = new Properties();
        // using properties.stringPropertyNames() because `result.putAll(properties)` not working when running inside
        // tomcat, if we put configuration in tomcat's catalina.properties
        // See:
        // - https://github.com/killbill/technical-support/issues/61
        // - https://github.com/killbill/technical-support/issues/67
        //
        // We have TestDefaultKillbillConfigSource#testGetProperties() that cover this, but seems like this is similar
        // to one of our chicken-egg problem? (see loadPropertiesFromFileOrSystemProperties() below)
        properties.stringPropertyNames().forEach(key -> result.setProperty(key, properties.getProperty(key)));

        RuntimeConfigRegistry.getAll().forEach((key, value) -> {
            if (!result.containsKey(key)) {
                result.setProperty(key, value);
            }
        });

        return result;
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

        return new Properties(System.getProperties());
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
                        synchronized (lock) {
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
                synchronized (lock) {
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
        properties.put(ENABLE_JASYPT_DECRYPTION, "false");
        properties.put(LOOKUP_ENVIRONMENT_VARIABLES, "true");
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

    private void overrideWithEnvironmentVariables() {
        // Find all Kill Bill properties in the environment variables
        final Map<String, String> env = System.getenv();
        for (final Entry<String, String> entry : env.entrySet()) {
            if (!entry.getKey().startsWith(ENVIRONMENT_VARIABLE_PREFIX)) {
                continue;
            }

            final String propertyName = fromEnvVariableName(entry.getKey());
            final String value = entry.getValue();
            properties.setProperty(propertyName, value);
        }
    }

    @VisibleForTesting
    String fromEnvVariableName(final String key) {
        return key.replace(ENVIRONMENT_VARIABLE_PREFIX, "").replaceAll("_", "\\.");
    }

    private void decryptJasyptProperties() {
        final String password = getEnvironmentVariable(JASYPT_ENCRYPTOR_PASSWORD_KEY, System.getProperty(JASYPT_ENCRYPTOR_PASSWORD_KEY));
        final String algorithm = getEnvironmentVariable(JASYPT_ENCRYPTOR_ALGORITHM_KEY, System.getProperty(JASYPT_ENCRYPTOR_ALGORITHM_KEY));

        final Enumeration<Object> keys = properties.keys();
        final StandardPBEStringEncryptor encryptor = initializeEncryptor(password, algorithm);
        // Iterate over all properties and decrypt ones that match
        while (keys.hasMoreElements()) {
            final String key = (String) keys.nextElement();
            final String value = (String) properties.get(key);
            final Optional<String> decryptableValue = decryptableValue(value);
            decryptableValue.ifPresent(s -> properties.setProperty(key, encryptor.decrypt(s)));
        }
    }

    private StandardPBEStringEncryptor initializeEncryptor(final String password, final String algorithm) {
        final StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();

        if (Strings.isNullOrEmpty(password)) {
            logger.error(JASYPT_ENCRYPTOR_PASSWORD_KEY + " is not set. Decrypting properties via Jasypt will likely fail.");
        }
        if (Strings.isNullOrEmpty(algorithm)) {
            logger.error(JASYPT_ENCRYPTOR_ALGORITHM_KEY + " is not set. Decrypting properties via Jasypt will likely fail.");
        }
        encryptor.setPassword(password);
        encryptor.setAlgorithm(algorithm);
        return encryptor;
    }

    private String getEnvironmentVariable(final String name, final String defaultValue) {
        String value = System.getenv(name);
        if (!Strings.isNullOrEmpty(value)) {
            return value;
        }

        value = getString(name);
        return Strings.isNullOrEmpty(value) ? defaultValue : value;
    }

    private Optional<String> decryptableValue(final String value) {
        if (value == null) {
            return Optional.empty();
        }

        final int start = value.indexOf(ENC_PREFIX);
        if (start != -1) {
            final int end = value.lastIndexOf(ENC_SUFFIX);
            if (end != -1) {
                return Optional.of(value.substring(start + ENC_PREFIX.length(), end));
            }
        }
        return Optional.empty();
    }
}
