/*
 * Copyright 2020-2025 Equinix, Inc
 * Copyright 2014-2025 The Billing Project, LLC
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

package org.killbill.billing.platform.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertiesWithSourceCollector {

    private volatile List<PropertyWithSource> properties = new ArrayList<>();
    private final Object lock = new Object();

    public void addProperties(final String source, final Map<String, String> props) {
        synchronized (lock) {
            final List<PropertyWithSource> updatedProperties = new ArrayList<>(properties);

            final Set<String> keysToAdd = props.keySet();
            updatedProperties.removeIf(p -> p.getSource().equals(source) && keysToAdd.contains(p.getKey()));

           /* props.forEach((key, value) ->
                                  updatedProperties.add(new PropertyWithSource(source, key, value)));*/

            props.forEach((s, s2) -> {
                if(s2 == null) {
                    System.out.println("Skipping2 adding a property " + s);
                } else {
                    updatedProperties.add(new PropertyWithSource(source, s, s2));
                }
            });

            this.properties = Collections.unmodifiableList(updatedProperties);
        }
    }

    public List<PropertyWithSource> getAllProperties() {
        return List.copyOf(properties);
    }

    public Map<String, List<PropertyWithSource>> getPropertiesBySource() {
        return properties.stream()
                         .collect(Collectors.groupingBy(
                                 PropertyWithSource::getSource,
                                 LinkedHashMap::new,
                                 Collectors.toList()));
    }
}