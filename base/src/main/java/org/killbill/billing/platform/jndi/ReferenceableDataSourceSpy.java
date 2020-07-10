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

package org.killbill.billing.platform.jndi;

import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.sql.DataSource;

import net.sf.log4jdbc.sql.jdbcapi.DataSourceSpy;

public class ReferenceableDataSourceSpy extends DataSourceSpy implements Referenceable {

    private final DataSource dataSource;
    private final String dataSourceId;

    public ReferenceableDataSourceSpy(final String dataSourceId) {
        super(DataSourceProxy.getDelegate(dataSourceId));
        this.dataSource = DataSourceProxy.getDelegate(dataSourceId);
        this.dataSourceId = dataSourceId;
    }

    public ReferenceableDataSourceSpy(final DataSource realDataSource, final String dataSourceId) {
        super(realDataSource);
        this.dataSource = realDataSource;
        this.dataSourceId = dataSourceId;

        DataSourceProxy.addDelegate(dataSourceId, realDataSource);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Reference getReference() throws NamingException {
        final Reference reference = new Reference(DataSourceProxy.class.getName(), ReferenceableDataSourceSpyFactory.class.getName(), null);

        reference.add(new StringRefAddr(ReferenceableDataSourceSpyFactory.DATA_SOURCE_ID, dataSourceId));

        return reference;
    }

    //@Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("javax.sql.DataSource.getParentLogger() is not currently supported by " + this.getClass().getName());
    }
}
