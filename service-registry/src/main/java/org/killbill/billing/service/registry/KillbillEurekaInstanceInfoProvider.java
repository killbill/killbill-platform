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

package org.killbill.billing.service.registry;

import java.util.Map;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.killbill.CreatorName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.InstanceInfo.PortType;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.appinfo.UniqueIdentifier;
import com.netflix.appinfo.providers.Archaius1VipAddressResolver;
import com.netflix.appinfo.providers.VipAddressResolver;

@Singleton
public class KillbillEurekaInstanceInfoProvider implements Provider<InstanceInfo> {

    private static final Logger logger = LoggerFactory.getLogger(KillbillEurekaInstanceInfoProvider.class);

    private final EurekaInstanceConfig eurekaConfig;

    private InstanceInfo instanceInfo;

    @Inject(optional = true)
    private VipAddressResolver vipAddressResolver = null;

    @Inject
    public KillbillEurekaInstanceInfoProvider(EurekaInstanceConfig eurekaConfig) {
        this.eurekaConfig = eurekaConfig;
    }

    @Override
    public synchronized InstanceInfo get() {
        if (instanceInfo == null) {
            // Build the lease information to be passed to the server based on eurekaConfig
            LeaseInfo.Builder leaseInfoBuilder = LeaseInfo.Builder.newBuilder()
                                                                  .setRenewalIntervalInSecs(eurekaConfig.getLeaseRenewalIntervalInSeconds())
                                                                  .setDurationInSecs(eurekaConfig.getLeaseExpirationDurationInSeconds());

            if (vipAddressResolver == null) {
                vipAddressResolver = new Archaius1VipAddressResolver();
            }

            // Builder the instance information to be registered with eureka server
            InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder(vipAddressResolver);

            // set the appropriate id for the InstanceInfo, falling back to datacenter Id if applicable, else hostname
            String instanceId = eurekaConfig.getInstanceId();
            DataCenterInfo dataCenterInfo = eurekaConfig.getDataCenterInfo();
            if (instanceId == null || instanceId.isEmpty()) {
                if (dataCenterInfo instanceof UniqueIdentifier) {
                    instanceId = ((UniqueIdentifier) dataCenterInfo).getId();
                } else {
                    instanceId = CreatorName.get();
                }
            }

            String hostName = eurekaConfig.getHostName(false);

            builder.setNamespace(eurekaConfig.getNamespace())
                   .setInstanceId(instanceId)
                   .setAppName(eurekaConfig.getAppname())
                   .setAppGroupName(eurekaConfig.getAppGroupName())
                   .setDataCenterInfo(eurekaConfig.getDataCenterInfo())
                   .setIPAddr(eurekaConfig.getIpAddress())
                   .setHostName(hostName)
                   .setPort(eurekaConfig.getNonSecurePort())
                   .enablePort(PortType.UNSECURE, eurekaConfig.isNonSecurePortEnabled())
                   .setSecurePort(eurekaConfig.getSecurePort())
                   .enablePort(PortType.SECURE, eurekaConfig.getSecurePortEnabled())
                   .setVIPAddress(eurekaConfig.getVirtualHostName())
                   .setSecureVIPAddress(eurekaConfig.getSecureVirtualHostName())
                   .setHomePageUrl(eurekaConfig.getHomePageUrlPath(), eurekaConfig.getHomePageUrl())
                   .setStatusPageUrl(eurekaConfig.getStatusPageUrlPath(), eurekaConfig.getStatusPageUrl())
                   .setASGName(eurekaConfig.getASGName())
                   .setHealthCheckUrls(eurekaConfig.getHealthCheckUrlPath(),
                                       eurekaConfig.getHealthCheckUrl(), eurekaConfig.getSecureHealthCheckUrl());


            // Start off with the STARTING state to avoid traffic
            if (!eurekaConfig.isInstanceEnabledOnit()) {
                InstanceStatus initialStatus = InstanceStatus.STARTING;
                logger.info("Setting initial instance status as: " + initialStatus);
                builder.setStatus(initialStatus);
            } else {
                logger.info("Setting initial instance status as: {}. This may be too early for the instance to advertise "
                            + "itself as available. You would instead want to control this via a healthcheck handler.",
                            InstanceStatus.UP);
            }

            // Add any user-specific metadata information
            for (Map.Entry<String, String> mapEntry : eurekaConfig.getMetadataMap().entrySet()) {
                String key = mapEntry.getKey();
                String value = mapEntry.getValue();
                builder.add(key, value);
            }

            instanceInfo = builder.build();
            instanceInfo.setLeaseInfo(leaseInfoBuilder.build());
        }
        return instanceInfo;
    }
}