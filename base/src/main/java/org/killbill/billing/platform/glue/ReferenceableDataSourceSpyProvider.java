/*
 * Copyright 2015-2017 Groupon, Inc
 * Copyright 2015-2017 The Billing Project, LLC
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

import javax.sql.DataSource;

import org.killbill.billing.platform.jndi.ReferenceableDataSourceSpy;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.jdbi.guice.DaoConfig;
import org.killbill.commons.jdbi.guice.DataSourceProvider;

public class ReferenceableDataSourceSpyProvider extends DataSourceProvider {

    public ReferenceableDataSourceSpyProvider(final DaoConfig config, final EmbeddedDB embeddedDB, final String poolName) {
        super(config, embeddedDB, poolName);
    }

    @Override
    public DataSource get() {
        final DataSource realDataSource = super.get();
        return new ReferenceableDataSourceSpy(realDataSource, poolName);
    }
}
