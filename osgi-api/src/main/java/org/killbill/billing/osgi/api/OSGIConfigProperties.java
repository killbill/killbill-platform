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

package org.killbill.billing.osgi.api;

import java.util.Properties;

/**
 * OSGI bundles should read their (system) properties using that service interface instead of using
 * the {@codeSystem.getProperties()} as there is no guarantees that standard java mechanism would work.
 */
public interface OSGIConfigProperties {

    /**
     * Retrieves the value of the given property.
     *
     * @param propertyName the system property name
     * @return the value of the property
     */
    public String getString(final String propertyName);

    /**
     * Returns all runtime resolved properties as a flat {@link Properties} object.
     *
     * @return all known configuration properties
     */
    Properties getProperties();

    /**
     * Returns all runtime resolved properties grouped by their respective source.
     * Each key in the outer map represents the source name (e.g., "SystemProperties", "KillbillServerConfig", "CatalogConfig"),
     * and the corresponding value is a map of property names to values loaded from that source.
     *
     * @return a map of configuration sources to their respective key-value property sets.
     */
   // Map<String, Map<String, String>> getPropertiesBySource();
}
