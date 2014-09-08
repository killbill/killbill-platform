/*
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.google.common.base.Preconditions;

public class DataSourceProxy implements DataSource {

    private static final Map<String, DataSource> delegates = new ConcurrentHashMap<String, DataSource>();

    public static <T extends DataSource> void addDelegate(final String dataSourceId, final T dataSource) {
        delegates.put(dataSourceId, dataSource);
    }

    public static DataSource getDelegate(final String dataSourceId) {
        return delegates.get(dataSourceId);
    }

    private final DataSource delegate;

    public DataSourceProxy(final String dataSourceId) {
        this.delegate = delegates.get(dataSourceId);
        Preconditions.checkNotNull(delegate, "DataSource delegate for id " + dataSourceId + " hasn't been configured!");
    }

    public DataSource getDelegate() {
        return delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getDelegate().getConnection();
    }

    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
        return getDelegate().getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return getDelegate().getLogWriter();
    }

    @Override
    public void setLogWriter(final PrintWriter out) throws SQLException {
        getDelegate().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(final int seconds) throws SQLException {
        getDelegate().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return getDelegate().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return getDelegate().getParentLogger();
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        return getDelegate().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return getDelegate().isWrapperFor(iface);
    }
}
