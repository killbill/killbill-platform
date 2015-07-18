/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
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

package org.killbill.billing.platform.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.sql.DataSource;

import org.killbill.billing.platform.jndi.ReferenceableDataSourceSpy;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.embeddeddb.GenericStandaloneDB;
import org.killbill.commons.embeddeddb.h2.H2EmbeddedDB;
import org.killbill.commons.embeddeddb.mysql.MySQLEmbeddedDB;
import org.killbill.commons.embeddeddb.mysql.MySQLStandaloneDB;
import org.killbill.commons.embeddeddb.postgresql.PostgreSQLEmbeddedDB;
import org.killbill.commons.embeddeddb.postgresql.PostgreSQLStandaloneDB;
import org.killbill.commons.jdbi.guice.DBIProvider;
import org.skife.jdbi.v2.IDBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;

public class PlatformDBTestingHelper {

    private static final Logger log = LoggerFactory.getLogger(PlatformDBTestingHelper.class);
    private static final String TEST_DATA_SOURCE_ID = "test";
    private static final String TEST_DB_PROPERTY_PREFIX = "org.killbill.billing.dbi.test.";

    protected EmbeddedDB instance;

    private static PlatformDBTestingHelper dbTestingHelper = null;

    public static synchronized PlatformDBTestingHelper get() {
        if (dbTestingHelper == null) {
            dbTestingHelper = new PlatformDBTestingHelper();
        }
        return dbTestingHelper;
    }

    protected PlatformDBTestingHelper() {
        if ("true".equals(System.getProperty(TEST_DB_PROPERTY_PREFIX + "h2"))) {
            log.info("Using h2 as the embedded database");
            instance = new H2EmbeddedDB();
        } else if ("true".equals(System.getProperty(TEST_DB_PROPERTY_PREFIX + "postgresql"))) {
            if (isUsingLocalInstance()) {
                log.info("Using PostgreSQL local database");
                final String databaseName = System.getProperty(TEST_DB_PROPERTY_PREFIX + "localDb.database", "killbill");
                final String username = System.getProperty(TEST_DB_PROPERTY_PREFIX + "localDb.username", "postgres");
                final String password = System.getProperty(TEST_DB_PROPERTY_PREFIX + "localDb.password", "postgres");
                instance = new PostgreSQLStandaloneDB(databaseName, username, password);
            } else {
                log.info("Using PostgreSQL as the embedded database");
                instance = new PostgreSQLEmbeddedDB();
            }
        } else {
            if (isUsingLocalInstance()) {
                log.info("Using MySQL local database");
                final String databaseName = System.getProperty(TEST_DB_PROPERTY_PREFIX + "localDb.database", "killbill");
                final String username = System.getProperty(TEST_DB_PROPERTY_PREFIX + "localDb.username", "root");
                final String password = System.getProperty(TEST_DB_PROPERTY_PREFIX + "localDb.password", "root");
                instance = new MySQLStandaloneDB(databaseName, username, password);
            } else {
                log.info("Using MySQL as the embedded database");
                instance = new MySQLEmbeddedDB();
            }
        }
    }

    public EmbeddedDB getInstance() {
        return instance;
    }

    public synchronized IDBI getDBI() throws IOException {

        final DataSource dataSource = getDataSource();
        return new DBIProvider(null, dataSource, null).get();
    }

    public DataSource getDataSource() throws IOException {
        final DataSource realDataSource = instance.getDataSource();
        return new ReferenceableDataSourceSpy(realDataSource, TEST_DATA_SOURCE_ID);
    }

    public synchronized void start() throws IOException {
        instance.initialize();
        instance.start();

        if (isUsingLocalInstance()) {
            return;
        }

        executeEngineSpecificScripts();

        executePostStartupScripts();

        instance.refreshTableNames();
    }

    protected synchronized void executePostStartupScripts() throws IOException {
        final String resourcesBase = "org/killbill/billing/beatrix";
        executePostStartupScripts(resourcesBase);
    }

    protected void executePostStartupScripts(final String resourcesBase) throws IOException {
        try {
            final String databaseSpecificDDL = streamToString(Resources.getResource(resourcesBase + "/" + "ddl-" + instance.getDBEngine().name().toLowerCase() + ".sql").openStream());
            instance.executeScript(databaseSpecificDDL);
        } catch (final IllegalArgumentException e) {
            // No engine-specific DDL
        }

        final String ddl = streamToString(Resources.getResource(resourcesBase + "/ddl.sql").openStream());
        instance.executeScript(ddl);
    }

    protected synchronized void executeEngineSpecificScripts() throws IOException {
        switch (instance.getDBEngine()) {
            case POSTGRESQL:
                final int port = URI.create(instance.getJdbcConnectionString().substring(5)).getPort();
                final GenericStandaloneDB postgreSQLDBConnection = new PostgreSQLStandaloneDB(instance.getDatabaseName(), "postgres", "postgres", "jdbc:postgresql://localhost:" + port + "/postgres");
                postgreSQLDBConnection.initialize();
                postgreSQLDBConnection.start();
                try {
                    // Setup permissions required by the PostgreSQL-specific DDL
                    postgreSQLDBConnection.executeScript("alter role " + instance.getUsername() + " with superuser;");
                } finally {
                    postgreSQLDBConnection.stop();
                }
                break;
            default:
                break;
        }
    }

    protected String streamToString(final InputStream inputStream) throws IOException {
        try {
            return new String(ByteStreams.toByteArray(inputStream), Charsets.UTF_8);
        } finally {
            inputStream.close();
        }
    }

    private boolean isUsingLocalInstance() {
        return (System.getProperty(TEST_DB_PROPERTY_PREFIX + "useLocalDb") != null);
    }
}
