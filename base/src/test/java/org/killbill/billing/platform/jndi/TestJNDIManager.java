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

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.embeddeddb.h2.H2EmbeddedDB;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.sf.log4jdbc.log.SpyLogFactory;

public class TestJNDIManager {

    EmbeddedDB embeddedDB;

    @BeforeMethod(groups = "slow")
    public void setUp() throws Exception {
        SpyLogFactory.loadSpyLogDelegator("net.sf.log4jdbc.log.slf4j.Slf4jSpyLogDelegator");

        final String databaseName = "killbillosgitests";
        embeddedDB = new H2EmbeddedDB(databaseName, UUID.randomUUID().toString(), UUID.randomUUID().toString(), "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_ON_EXIT=FALSE");
        embeddedDB.initialize();
        embeddedDB.start();
    }

    @AfterMethod(groups = "slow")
    public void tearDown() throws Exception {
        embeddedDB.stop();
    }

    @Test(groups = "slow")
    public void testExportAndLookup() throws NamingException, IOException, SQLException {
        final JNDIManager jndiManager = new JNDIManager();

        // JdbcConnectionPool is not serializable unfortunately. Tests using JNDI won't work on H2 (we don't have any yet)
        //final JdbcConnectionPool jdbcConnectionPool = (JdbcConnectionPool) embeddedDB.getDataSource();
        //final ReferenceableDataSourceSpy<JdbcConnectionPool> retrievedJdbcConnectionPool = testForDataSource(jndiManager, new ReferenceableDataSourceSpy<JdbcConnectionPool>(jdbcConnectionPool), ReferenceableDataSourceSpy.class);
        //Assert.assertNotNull(retrievedJdbcConnectionPool.getDataSource().getConnection());

        // JdbcDataSource is Referenceable
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(embeddedDB.getJdbcConnectionString());
        dataSource.setUser(embeddedDB.getUsername());
        dataSource.setPassword(embeddedDB.getPassword());
        final JdbcDataSource retrievedJdbcDataSource = testForDataSource(jndiManager, dataSource, JdbcDataSource.class);
        Assert.assertEquals(retrievedJdbcDataSource.getURL(), embeddedDB.getJdbcConnectionString());
        Assert.assertEquals(retrievedJdbcDataSource.getUser(), embeddedDB.getUsername());
        Assert.assertEquals(retrievedJdbcDataSource.getPassword(), embeddedDB.getPassword());
        Assert.assertNotNull(retrievedJdbcDataSource.getConnection());

        // Try to wrap around a DataSourceSpy
        final ReferenceableDataSourceSpy retrievedReferenceableDataSourceSpy = testForDataSource(jndiManager, new ReferenceableDataSourceSpy(dataSource, "something"), ReferenceableDataSourceSpy.class);
        final DataSource retrievedJdbcDataSource2Delegate = retrievedReferenceableDataSourceSpy.getDataSource();
        Assert.assertTrue(retrievedJdbcDataSource2Delegate instanceof JdbcDataSource);
        final JdbcDataSource retrievedJdbcDataSource2 = (JdbcDataSource) retrievedJdbcDataSource2Delegate;
        Assert.assertEquals(retrievedJdbcDataSource2.getURL(), embeddedDB.getJdbcConnectionString());
        Assert.assertEquals(retrievedJdbcDataSource2.getUser(), embeddedDB.getUsername());
        Assert.assertEquals(retrievedJdbcDataSource2.getPassword(), embeddedDB.getPassword());
        Assert.assertNotNull(retrievedJdbcDataSource2.getConnection());
    }

    private <T> T testForDataSource(final JNDIManager jndiManager, final DataSource dataSource, final Class<T> klass) {
        final String name = "a/b/c";
        jndiManager.export(name, dataSource);

        final Object retrievedDataSourceObject = jndiManager.lookup(name);
        Assert.assertTrue(klass.isInstance(retrievedDataSourceObject), klass + " is not an instance of " + retrievedDataSourceObject);

        return (T) retrievedDataSourceObject;
    }
}
