/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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

package org.killbill.billing.service.registry;

import org.killbill.billing.server.healthchecks.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;

public class EurekaServiceRegistry implements ServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(EurekaServiceRegistry.class);

    private ApplicationInfoManager applicationInfoManager;

    public static final String EUREKA_SERVICE_NAME = "eureka-service";

    @Inject
    public EurekaServiceRegistry(ApplicationInfoManager applicationInfoManager) {
        this.applicationInfoManager = applicationInfoManager;
    }

    @Override
    public void register() {
        logger.info("--------------> Setting Eureka Status to UP <----------------");
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        logger.info("--------------> Finished setting Eureka Status to UP <----------------");
    }

    @Override
    public void unregister() {
        logger.info("--------------> Setting Eureka Status to DOWN <----------------");
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
        logger.info("--------------> Finished setting Eureka Status to DOWN <----------------");
    }
}
