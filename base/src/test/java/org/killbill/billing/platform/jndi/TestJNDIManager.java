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
import java.util.UUID;

import javax.naming.NamingException;

import org.h2.jdbcx.JdbcDataSource;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.embeddeddb.h2.H2EmbeddedDB;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestJNDIManager {

    EmbeddedDB embeddedDB;

    @BeforeMethod(groups = "slow")
    public void setUp() throws Exception {
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
    public void testExportAndLookup() throws NamingException, IOException {
        final JNDIManager jndiManager = new JNDIManager();

        // We cannot use embeddedDB.getDataSource() which returns a JdbcConnectionPool and is not Referenceable
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(embeddedDB.getJdbcConnectionString());
        dataSource.setUser(embeddedDB.getUsername());
        dataSource.setPassword(embeddedDB.getPassword());

        final String name = "a/b/c";
        jndiManager.export(name, dataSource);

        final Object retrievedDataSourceObject = jndiManager.lookup(name);
        Assert.assertTrue(retrievedDataSourceObject instanceof JdbcDataSource);
        final JdbcDataSource retrievedDataSource = (JdbcDataSource) retrievedDataSourceObject;
        Assert.assertEquals(retrievedDataSource.getURL(), embeddedDB.getJdbcConnectionString());
        Assert.assertEquals(retrievedDataSource.getUser(), embeddedDB.getUsername());
        Assert.assertEquals(retrievedDataSource.getPassword(), embeddedDB.getPassword());
    }
}
