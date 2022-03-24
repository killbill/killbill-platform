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

package org.killbill.billing.server.healthchecks;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.osgi.api.Healthcheck.HealthStatus;
import org.killbill.billing.osgi.api.OSGIServiceRegistration;
import org.killbill.commons.health.api.HealthCheck;
import org.killbill.commons.health.api.Result;
import org.killbill.commons.health.impl.HealthyResultBuilder;
import org.killbill.commons.health.impl.UnhealthyResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@Singleton
public class KillbillPluginsHealthcheck implements HealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(KillbillPluginsHealthcheck.class);

    private OSGIServiceRegistration<Healthcheck> pluginHealthchecks = null;

    @Inject(optional = true)
    public void setPluginHealthchecks(final OSGIServiceRegistration<Healthcheck> pluginHealthchecks) {
        this.pluginHealthchecks = pluginHealthchecks;
    }

    @Override
    public Result check() {
        final Map<String, Object> details = new HashMap<>();
        boolean isHealthy = true;
        if (pluginHealthchecks != null) {
            for (final String pluginHealthcheckService : pluginHealthchecks.getAllServices()) {
                final Healthcheck pluginHealthcheck = pluginHealthchecks.getServiceForName(pluginHealthcheckService);
                if (pluginHealthcheck == null) {
                    continue;
                }
                final HealthStatus pluginStatus = pluginHealthcheck.getHealthStatus(null, null);
                details.put(pluginHealthcheckService, pluginStatus.getDetails());
                isHealthy = isHealthy && pluginStatus.isHealthy();
            }
        }

        if (isHealthy) {
            return new HealthyResultBuilder().setDetails(details).createHealthyResult();
        } else {
            return new UnhealthyResultBuilder().setDetails(details).createUnhealthyResult();
        }
    }
}
