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

package org.killbill.billing.server.modules;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.killbill.billing.server.dao.EmbeddedDBFactory;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.embeddeddb.EmbeddedDB.DBEngine;
import org.killbill.commons.jdbi.guice.DaoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.google.inject.Provider;

public class EmbeddedDBProvider implements Provider<EmbeddedDB> {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedDBProvider.class);

    private final DaoConfig config;

    @Inject
    public EmbeddedDBProvider(final DaoConfig config) {
        this.config = config;
    }

    @Override
    public EmbeddedDB get() {
        final EmbeddedDB embeddedDB = EmbeddedDBFactory.get(config);

        if (DBEngine.H2.equals(embeddedDB.getDBEngine())) {
            try {
                // Standalone mode?
                initializeEmbeddedDB(embeddedDB);
            } catch (final IOException e) {
                logger.error("Error while initializing H2, opportunistically continuing the startup sequence", e);
            } catch (final SQLException e) {
                logger.error("Error while initializing H2, opportunistically continuing the startup sequence", e);
            }
        }

        return embeddedDB;
    }

    protected void initializeEmbeddedDB(final EmbeddedDB embeddedDB) throws IOException, SQLException {
        embeddedDB.initialize();
        embeddedDB.start();

        // If the tables have not been created yet, do it, otherwise don't clobber them
        if (!embeddedDB.getAllTables().isEmpty()) {
            return;
        }

        for (final String ddlFile : getDDLFiles()) {
            final URL resource;
            try {
                final URI uri = new URI(ddlFile);
                final String scheme = uri.getScheme();
                resource = scheme == null ? Resources.getResource(ddlFile) : new File(uri.getSchemeSpecificPart()).toURI().toURL();
            } catch (final URISyntaxException e) {
                throw new IllegalStateException(e);
            }

            final InputStream inputStream = resource.openStream();
            try {
                final String ddl = streamToString(inputStream);
                embeddedDB.executeScript(ddl);
            } finally {
                inputStream.close();
            }
        }
        embeddedDB.refreshTableNames();
    }

    protected Iterable<String> getDDLFiles() {
        final String seedFile = System.getProperty("org.killbill.dao.seedFile");
        if (seedFile == null) {
            return ImmutableList.<String>of();
        }
        final List<String> ddlFiles = new LinkedList<>();
        ddlFiles.add(seedFile);
        ddlFiles.addAll(List.of(System.getProperty("org.killbill.dao.additionalSeedFiles", "").split(",")));
        ddlFiles.removeIf(String::isEmpty);
        return ddlFiles;
    }

    protected String streamToString(final InputStream inputStream) throws IOException {
        return new String(ByteStreams.toByteArray(inputStream), Charsets.UTF_8);
    }
}
