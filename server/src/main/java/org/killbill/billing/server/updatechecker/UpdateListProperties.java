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

package org.killbill.billing.server.updatechecker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.killbill.commons.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateListProperties {

    private static final Logger log = LoggerFactory.getLogger(UpdateListProperties.class);

    private final Properties properties = new Properties();

    public UpdateListProperties(final URL updateCheckURL, final int connectionTimeout) {
        try {
            loadUpdateListProperties(updateCheckURL, connectionTimeout);
        } catch (final IOException e) {
            log.debug("Unable to load update list properties", e);
        }
    }

    public String getGeneralNotice() {
        return getProperty("general.notice");
    }

    public String getNoticeForVersion(final String version) {
        return getProperty(version + ".notice");
    }

    public List<String> getUpdatesForVersion(final String version) {
        final String updates = getProperty(version + ".updates");
        return updates == null ? Collections.emptyList() : Strings.split(updates, ",");
    }

    public String getReleaseNotesForVersion(final String version) {
        return getProperty(version + ".release-notes");
    }

    private String getProperty(final String key) {
        return getSanitizedString(properties.getProperty(key));
    }

    private String getSanitizedString(final String string) {
        return Strings.isNullOrEmpty(string) ? null : string.trim();
    }

    private void loadUpdateListProperties(final URL updateCheckURL, final int connectionTimeout) throws IOException {
        log.debug("Checking {} for updates", updateCheckURL.toExternalForm());
        final URLConnection connection = updateCheckURL.openConnection();
        connection.setConnectTimeout(connectionTimeout);

        final InputStream in = connection.getInputStream();
        try {
            properties.load(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
