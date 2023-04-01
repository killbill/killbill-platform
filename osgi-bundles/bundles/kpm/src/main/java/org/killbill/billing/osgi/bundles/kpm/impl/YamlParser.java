/*
 * Copyright 2020-2023 Equinix, Inc
 * Copyright 2014-2023 The Billing Project, LLC
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

package org.killbill.billing.osgi.bundles.kpm.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;

import org.killbill.commons.utils.collect.Iterables;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.inspector.TrustedTagInspector;

class YamlParser {

    private final Yaml yaml;
    private final Path path;

    YamlParser(final Path yamlFile) {
        this.yaml = createYaml();
        this.path = yamlFile;
    }

    private static Yaml createYaml() {
        final LoaderOptions options = new LoaderOptions();
        options.setTagInspector(new TrustedTagInspector());
        return new Yaml(options);
    }

    Set<Object> loadAll() {
        try (final InputStream is = new FileInputStream(path.toFile())) {
            return Iterables.toUnmodifiableSet(yaml.loadAll(is));
        } catch (final IOException e) {
            throw new RuntimeException("Unable to parse yaml file: " + path.toAbsolutePath(), e);
        }
    }

}
