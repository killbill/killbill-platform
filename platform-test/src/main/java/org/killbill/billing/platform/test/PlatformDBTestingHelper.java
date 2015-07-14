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

package org.killbill.billing.platform.test;

import java.io.IOException;
import java.io.InputStream;

import javax.sql.DataSource;

import org.killbill.billing.platform.jndi.ReferenceableDataSourceSpy;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.embeddeddb.h2.H2EmbeddedDB;
import org.killbill.commons.embeddeddb.mysql.MySQLEmbeddedDB;
import org.killbill.commons.embeddeddb.mysql.MySQLStandaloneDB;
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

    protected EmbeddedDB instance;

    private static PlatformDBTestingHelper dbTestingHelper = null;

    public static synchronized PlatformDBTestingHelper get() {
        if (dbTestingHelper == null) {
            dbTestingHelper = new PlatformDBTestingHelper();
        }
        return dbTestingHelper;
    }

    protected PlatformDBTestingHelper() {
        if ("true".equals(System.getProperty("org.killbill.billing.dbi.test.h2"))) {
            log.info("Using h2 as the embedded database");
            instance = new H2EmbeddedDB();
        } else if ("true".equals(System.getProperty("org.killbill.billing.dbi.test.postgresql"))) {
            if (isUsingLocalInstance()) {
                log.info("Using postgresql local database");
                final String databaseName = System.getProperty("org.killbill.billing.dbi.test.localDb.database", "postgres");
                final String username = System.getProperty("org.killbill.billing.dbi.test.localDb.username", "postgres");
                final String password = System.getProperty("org.killbill.billing.dbi.test.localDb.password", "postgres");
                instance = new PostgreSQLStandaloneDB(databaseName, username, password);
            } else {
                throw new UnsupportedOperationException("PostgreSQL can be chosen for stand-alone mode; set org.killbill.billing.dbi.test.useLocalDb to true.");
            }
        } else {
            if (isUsingLocalInstance()) {
                log.info("Using MySQL local database");
                final String databaseName = System.getProperty("org.killbill.billing.dbi.test.localDb.database", "killbill");
                final String username = System.getProperty("org.killbill.billing.dbi.test.localDb.username", "root");
                final String password = System.getProperty("org.killbill.billing.dbi.test.localDb.password", "root");
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

        executePostStartupScripts();

        instance.refreshTableNames();
    }

    protected synchronized void executePostStartupScripts() throws IOException {
        final String ddl = streamToString(Resources.getResource("org/killbill/billing/beatrix/ddl.sql").openStream());
        instance.executeScript(ddl);
    }

    protected String streamToString(final InputStream inputStream) throws IOException {
        try {
            return new String(ByteStreams.toByteArray(inputStream), Charsets.UTF_8);
        } finally {
            inputStream.close();
        }
    }

    private boolean isUsingLocalInstance() {
        return (System.getProperty("org.killbill.billing.dbi.test.useLocalDb") != null);
    }
}
