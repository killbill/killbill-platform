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

package org.killbill.billing.osgi.bundles.eureka;

import java.util.Hashtable;
import java.util.Map;

import org.killbill.CreatorName;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.api.ServiceRegistry;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.InstanceInfo.PortType;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.appinfo.UniqueIdentifier;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.CommonConstants;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.EurekaClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Activator extends KillbillActivatorBase {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    public static final String BUNDLE_NAME = "killbill-eureka";

    private EurekaClient client;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        // Load properties
        ConfigurationManager.loadProperties(configProperties.getProperties());
        final String namespace = MoreObjects.firstNonNull(configProperties.getProperties().getProperty("eureka.namespace"), CommonConstants.DEFAULT_CONFIG_NAMESPACE);

        final KillbillEurekaInstanceConfig eurekaInstanceConfig = new KillbillEurekaInstanceConfig(namespace);
        final DefaultEurekaClientConfig eurekaClientConfig = new DefaultEurekaClientConfig(namespace);
        // TODO: Remove this when DiscoveryManager is finally no longer used
        DiscoveryManager.getInstance().setEurekaInstanceConfig(eurekaInstanceConfig);
        DiscoveryManager.getInstance().setEurekaClientConfig(eurekaClientConfig);

        final InstanceInfo instanceInfo = createInstanceInfo(eurekaInstanceConfig);

        final ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(eurekaInstanceConfig, instanceInfo);

        client = new DiscoveryClient(applicationInfoManager, eurekaClientConfig);

        final EurekaServiceRegistry serviceRegistry = new EurekaServiceRegistry(applicationInfoManager);
        registerServiceRegistry(context, serviceRegistry);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        try {
            client.shutdown();
        } finally {
            super.stop(context);
        }
    }

    private void registerServiceRegistry(final BundleContext context, final ServiceRegistry serviceRegistry) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, BUNDLE_NAME);
        registrar.registerService(context, ServiceRegistry.class, serviceRegistry, props);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    private InstanceInfo createInstanceInfo(final EurekaInstanceConfig eurekaConfig) {
        // Build the lease information to be passed to the server based on eurekaConfig
        final LeaseInfo.Builder leaseInfoBuilder = LeaseInfo.Builder.newBuilder()
                                                                    .setRenewalIntervalInSecs(eurekaConfig.getLeaseRenewalIntervalInSeconds())
                                                                    .setDurationInSecs(eurekaConfig.getLeaseExpirationDurationInSeconds());

        // Builder the instance information to be registered with eureka server
        final InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder();

        // Set the appropriate id for the InstanceInfo, falling back to datacenter Id if applicable, else hostname
        String instanceId = eurekaConfig.getInstanceId();
        final DataCenterInfo dataCenterInfo = eurekaConfig.getDataCenterInfo();
        if (instanceId == null || instanceId.isEmpty()) {
            if (dataCenterInfo instanceof UniqueIdentifier) {
                instanceId = ((UniqueIdentifier) dataCenterInfo).getId();
            } else {
                instanceId = CreatorName.get();
            }
        }

        final String hostName = eurekaConfig.getHostName(false);

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
               .setHealthCheckUrls(eurekaConfig.getHealthCheckUrlPath(), eurekaConfig.getHealthCheckUrl(), eurekaConfig.getSecureHealthCheckUrl());

        // Start off with the STARTING state to avoid traffic
        if (!eurekaConfig.isInstanceEnabledOnit()) {
            final InstanceStatus initialStatus = InstanceStatus.STARTING;
            logger.info("Setting initial instance status as: {}", initialStatus);
            builder.setStatus(initialStatus);
        } else {
            logger.info("Setting initial instance status as: {}. This may be too early for the instance to advertise " + "itself as available. You would instead want to control this via a healthcheck handler.", InstanceStatus.UP);
        }

        // Add any user-specific metadata information
        for (final Map.Entry<String, String> mapEntry : eurekaConfig.getMetadataMap().entrySet()) {
            final String key = mapEntry.getKey();
            final String value = mapEntry.getValue();
            builder.add(key, value);
        }

        final InstanceInfo instanceInfo = builder.build();
        instanceInfo.setLeaseInfo(leaseInfoBuilder.build());

        return instanceInfo;
    }
}
