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

import org.killbill.billing.platform.api.KillbillConfigSource;
import org.skife.config.ConfigSource;

import com.google.inject.AbstractModule;

public abstract class KillBillPlatformModuleBase extends AbstractModule {

    public static final String MAIN_DATA_SOURCE_ID = "main";
    public static final String MAIN_RO_DATA_SOURCE_ID = "main-ro";
    public static final String SHIRO_DATA_SOURCE_ID = "shiro";
    public static final String OSGI_DATA_SOURCE_ID = "osgi";

    protected final KillbillConfigSource configSource;
    protected final ConfigSource skifeConfigSource;

    public KillBillPlatformModuleBase(final KillbillConfigSource configSource) {
        this.configSource = configSource;
        this.skifeConfigSource = new KillbillSkifeConfigSource(configSource);
    }

    private static final class KillbillSkifeConfigSource implements ConfigSource {

        private final KillbillConfigSource configSource;

        private KillbillSkifeConfigSource(final KillbillConfigSource configSource) {
            this.configSource = configSource;
        }

        @Override
        public String getString(final String propertyName) {
            if(propertyName == null) {
                return null;
            }

            final String test = configSource.getString(propertyName);

            if(test == null) {
                System.out.println("getString.. is null for property " + propertyName);
            }

            return test;
        }
    }
}
