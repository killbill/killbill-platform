/*
 * Copyright 2010-2013 Ning, Inc.
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

package org.killbill.billing.osgi.glue;

import org.killbill.commons.jdbi.guice.DaoConfig;
import org.killbill.commons.jdbi.guice.DataSourceConnectionPoolingType;
import org.killbill.commons.jdbi.log.LogLevel;
import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.DefaultNull;
import org.skife.config.Description;
import org.skife.config.TimeSpan;

public interface OSGIDataSourceConfig extends DaoConfig {

    static final String DATA_SOURCE_PROP_PREFIX = "org.killbill.billing.osgi.dao.";

    @Override
    @Description("The jdbc url for the database")
    @Config(DATA_SOURCE_PROP_PREFIX + "url")
    @Default("jdbc:h2:file:/var/tmp/killbill;MODE=MYSQL;DB_CLOSE_DELAY=-1;MVCC=true;DB_CLOSE_ON_EXIT=FALSE")
    String getJdbcUrl();

    @Override
    @Description("The jdbc user name for the database")
    @Config(DATA_SOURCE_PROP_PREFIX + "user")
    @Default("killbill")
    String getUsername();

    @Override
    @Description("The jdbc password for the database")
    @Config(DATA_SOURCE_PROP_PREFIX + "password")
    @Default("killbill")
    String getPassword();

    @Override
    @Description("The minimum allowed number of idle connections to the database")
    @Config(DATA_SOURCE_PROP_PREFIX + "minIdle")
    @Default("1")
    int getMinIdle();

    @Override
    @Description("The maximum allowed number of active connections to the database")
    @Config(DATA_SOURCE_PROP_PREFIX + "maxActive")
    @Default("100")
    int getMaxActive();

    @Override
    @Description("How long to wait before a connection attempt to the database is considered timed out")
    @Config(DATA_SOURCE_PROP_PREFIX + "connectionTimeout")
    @Default("10s")
    TimeSpan getConnectionTimeout();

    @Override
    @Description("The time for a connection to remain unused before it is closed off")
    @Config(DATA_SOURCE_PROP_PREFIX + "idleMaxAge")
    @Default("60m")
    TimeSpan getIdleMaxAge();

    @Override
    @Description("Any connections older than this setting will be closed off whether it is idle or not. Connections " +
                 "currently in use will not be affected until they are returned to the pool")
    @Config(DATA_SOURCE_PROP_PREFIX + "maxConnectionAge")
    @Default("0m")
    TimeSpan getMaxConnectionAge();

    @Override
    @Description("Time for a connection to remain idle before sending a test query to the DB")
    @Config(DATA_SOURCE_PROP_PREFIX + "idleConnectionTestPeriod")
    @Default("5m")
    TimeSpan getIdleConnectionTestPeriod();

    @Description("Number of prepared statements that the driver will cache per connection")
    @Config(DATA_SOURCE_PROP_PREFIX + "prepStmtCacheSize")
    @Default("500")
    int getPreparedStatementsCacheSize();

    @Description("Maximum length of a prepared SQL statement that the driver will cache")
    @Config(DATA_SOURCE_PROP_PREFIX + "prepStmtCacheSqlLimit")
    @Default("2048")
    int getPreparedStatementsCacheSqlLimit();

    @Description("Enable prepared statements cache")
    @Config(DATA_SOURCE_PROP_PREFIX + "cachePrepStmts")
    @Default("true")
    boolean isPreparedStatementsCacheEnabled();

    @Description("Enable server-side prepared statements")
    @Config(DATA_SOURCE_PROP_PREFIX + "useServerPrepStmts")
    @Default("true")
    boolean isServerSidePreparedStatementsEnabled();

    @Description("DataSource class name provided by the JDBC driver, leave null for autodetection")
    @Config(DATA_SOURCE_PROP_PREFIX + "dataSourceClassName")
    @DefaultNull
    String getDataSourceClassName();

    @Description("JDBC driver to use (when dataSourceClassName is null)")
    @Config(DATA_SOURCE_PROP_PREFIX + "driverClassName")
    @DefaultNull
    String getDriverClassName();

    @Description("MySQL server version")
    @Config(DATA_SOURCE_PROP_PREFIX + "mysqlServerVersion")
    @Default("4.0")
    String getMySQLServerVersion();

    @Override
    @Description("Log level for SQL queries")
    @Config(DATA_SOURCE_PROP_PREFIX + "logLevel")
    @Default("DEBUG")
    LogLevel getLogLevel();

    @Override
    @Description("Connection pooling type")
    @Config(DATA_SOURCE_PROP_PREFIX + "poolingType")
    @Default("HIKARICP")
    DataSourceConnectionPoolingType getConnectionPoolingType();
}
