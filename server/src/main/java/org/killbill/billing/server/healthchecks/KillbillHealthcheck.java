/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014-2017 Groupon, Inc
 * Copyright 2014-2017 The Billing Project, LLC
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weakref.jmx.Managed;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Optional;
import com.google.inject.Inject;

@Singleton
public class KillbillHealthcheck extends HealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(KillbillHealthcheck.class);

    private static final String OK = "OK";
    private static final String OUT_OF_ROTATION = "Out of rotation";

    private final AtomicBoolean outOfRotation = new AtomicBoolean(true);
    private Set<ServiceRegistry> serviceRegistries = Collections.emptySet();

    @Inject(optional=true)
    public void setServiceRegistries(Set<ServiceRegistry> serviceRegistries) {
        this.serviceRegistries = serviceRegistries;
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

        for (ServiceRegistry serviceRegistry : serviceRegistries) {
            logger.info("Registering ServiceRegistry {}", serviceRegistry);
            try {
                serviceRegistry.register();
            } catch (RuntimeException e) {
                logger.warn("Failed to register ServiceRegistry {}. Exception: {}", serviceRegistry, e);
            }
        }
    }

    @Managed(description = "Put out of rotation")
    public void putOutOfRotation() {
        logger.warn("Putting host out of rotation");
        outOfRotation.set(true);

        for (ServiceRegistry serviceRegistry : serviceRegistries) {
            logger.info("Unregistering ServiceRegistry {}", serviceRegistry);
            try {
                serviceRegistry.unregister();
            } catch (RuntimeException e) {
                logger.warn("Failed to unregister ServiceRegistry {}. Exception: {}", serviceRegistry, e);
            }
        }
    }

    private Result buildHealthcheckResponse(final boolean healthy, final String message) {
        final ResultBuilder resultBuilder = Result.builder();
        if (healthy) {
            resultBuilder.healthy();
        } else {
            resultBuilder.unhealthy();
        }
        return resultBuilder.withMessage(message).build();
    }
}
