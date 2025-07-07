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

package org.killbill.billing.platform.glue;

import java.util.Map;

import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.notificationq.DefaultNotificationQueueService;
import org.killbill.notificationq.api.NotificationQueueConfig;
import org.killbill.notificationq.api.NotificationQueueService;
import org.skife.config.AugmentedConfigurationObjectFactory;

public class NotificationQueueModule extends KillBillPlatformModuleBase {

    public NotificationQueueModule(final KillbillConfigSource configSource) {
        super(configSource);
    }

    @Override
    protected void configure() {
        configureNotificationQueueService();
        configureNotificationQueueConfig();
    }

    protected void configureNotificationQueueService() {
        bind(NotificationQueueService.class).to(DefaultNotificationQueueService.class).asEagerSingleton();
    }

    protected void configureNotificationQueueConfig() {
        final NotificationQueueConfig config = new AugmentedConfigurationObjectFactory(skifeConfigSource).buildWithReplacements(NotificationQueueConfig.class,
                                                                                                                       Map.of("instanceName", "main"));
        bind(NotificationQueueConfig.class).toInstance(config);
    }
}
