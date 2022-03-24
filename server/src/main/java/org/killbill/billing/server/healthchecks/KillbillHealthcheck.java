/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2022 Equinix, Inc
 * Copyright 2014-2022 The Billing Project, LLC
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

package org.killbill.billing.server.healthchecks;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Singleton;

import org.killbill.billing.osgi.api.OSGIServiceRegistration;
import org.killbill.billing.osgi.api.ServiceRegistry;
import org.killbill.commons.health.api.HealthCheck;
import org.killbill.commons.health.api.Result;
import org.killbill.commons.health.impl.HealthyResultBuilder;
import org.killbill.commons.health.impl.UnhealthyResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weakref.jmx.Managed;

import com.google.inject.Inject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Singleton
public class KillbillHealthcheck implements HealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(KillbillHealthcheck.class);

    private static final String OK = "OK";
    private static final String OUT_OF_ROTATION = "Out of rotation";

    private final AtomicBoolean outOfRotation = new AtomicBoolean(true);
    private Set<ServiceRegistry> serviceRegistries = Collections.emptySet();
    private OSGIServiceRegistration<ServiceRegistry> pluginServiceRegistries = null;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    @Inject(optional = true)
    public void setServiceRegistries(final Set<ServiceRegistry> serviceRegistries) {
        this.serviceRegistries = serviceRegistries;
    }

    @Inject
    public void setPluginServiceRegistries(final OSGIServiceRegistration<ServiceRegistry> pluginServiceRegistries) {
        this.pluginServiceRegistries = pluginServiceRegistries;
    }

    @Override
    public Result check() {
        if (outOfRotation.get()) {
            return buildHealthcheckResponse(false, OUT_OF_ROTATION);
        } else {
            return buildHealthcheckResponse(true, OK);
        }
    }

    @Managed(description = "Basic killbill healthcheck")
    public boolean isHealthy() {
        return check().isHealthy();
    }

    @Managed(description = "Put in rotation")
    public void putInRotation() {
        logger.warn("Putting host in rotation");
        outOfRotation.set(false);

        for (final ServiceRegistry serviceRegistry : serviceRegistries) {
            logger.info("Registering ServiceRegistry {}", serviceRegistry);
            try {
                serviceRegistry.register();
            } catch (final RuntimeException e) {
                logger.warn("Failed to register ServiceRegistry {}. Exception: {}", serviceRegistry, e);
            }
        }

        if (pluginServiceRegistries != null) {
            for (final String pluginServiceRegistryService : pluginServiceRegistries.getAllServices()) {
                final ServiceRegistry pluginServiceRegistry = pluginServiceRegistries.getServiceForName(pluginServiceRegistryService);
                if (pluginServiceRegistry == null) {
                    continue;
                }

                logger.info("Registering Plugin ServiceRegistry {}", pluginServiceRegistry);
                try {
                    pluginServiceRegistry.register();
                } catch (final RuntimeException e) {
                    logger.warn("Failed to register Plugin ServiceRegistry {}. Exception: {}", pluginServiceRegistry, e);
                }
            }
        }
    }

    @Managed(description = "Put out of rotation")
    public void putOutOfRotation() {
        logger.warn("Putting host out of rotation");
        outOfRotation.set(true);

        for (final ServiceRegistry serviceRegistry : serviceRegistries) {
            logger.info("Unregistering ServiceRegistry {}", serviceRegistry);
            try {
                serviceRegistry.unregister();
            } catch (final RuntimeException e) {
                logger.warn("Failed to unregister ServiceRegistry {}. Exception: {}", serviceRegistry, e);
            }
        }

        if (pluginServiceRegistries != null) {
            for (final String pluginServiceRegistryService : pluginServiceRegistries.getAllServices()) {
                final ServiceRegistry pluginServiceRegistry = pluginServiceRegistries.getServiceForName(pluginServiceRegistryService);
                if (pluginServiceRegistry == null) {
                    continue;
                }

                logger.info("Unregistering Plugin ServiceRegistry {}", pluginServiceRegistry);
                try {
                    pluginServiceRegistry.unregister();
                } catch (final RuntimeException e) {
                    logger.warn("Failed to unregister Plugin ServiceRegistry {}. Exception: {}", pluginServiceRegistry, e);
                }
            }
        }
    }

    private Result buildHealthcheckResponse(final boolean healthy, final String message) {
        if (healthy) {
            return new HealthyResultBuilder().setMessage(message).createHealthyResult();
        } else {
            return new UnhealthyResultBuilder().setMessage(message).createUnhealthyResult();
        }
    }
}
