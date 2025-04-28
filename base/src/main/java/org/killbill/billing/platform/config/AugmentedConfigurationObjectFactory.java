/*
 * Copyright 2020-2025 Equinix, Inc
 * Copyright 2014-2025 The Billing Project, LLC
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.skife.config.Config;
import org.skife.config.ConfigSource;
import org.skife.config.ConfigurationObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Augmented version of {@link ConfigurationObjectFactory} that collects resolved config properties
 * at runtime and registers them in {@link DefaultKillbillConfigSource}.
 */
public class AugmentedConfigurationObjectFactory extends ConfigurationObjectFactory {

    private static final Logger log = LoggerFactory.getLogger(AugmentedConfigurationObjectFactory.class);

    public AugmentedConfigurationObjectFactory(final ConfigSource configSource) {
        super(configSource);
    }

    @Override
    public <T> T build(final Class<T> configClass) {
        final T instance = super.build(configClass);
        collectConfigValues(configClass, instance);

        return instance;
    }

    private <T> void collectConfigValues(final Class<T> configClass, final T instance) {
        for (final Method method : configClass.getMethods()) {
            final Config configAnnotation = method.getAnnotation(Config.class);
            if (configAnnotation != null) {
                try {
                    final Object value = method.invoke(instance);
                    final String[] keys = configAnnotation.value();
                    Arrays.stream(keys)
                          .forEach(key -> DefaultKillbillConfigSource.addRuntimeProperties(key, value));

                } catch (final IllegalAccessException | InvocationTargetException e) {
                    log.warn("Failed to resolve config method: {}", method.getName(), e);
                }
            }
        }
    }
}
