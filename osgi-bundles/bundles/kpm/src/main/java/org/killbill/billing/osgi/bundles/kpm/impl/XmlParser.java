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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

final class XmlParser {

    private final Logger logger = LoggerFactory.getLogger(XmlParser.class);
    private final JsonNode root;

    XmlParser(final Path path) throws IOException {
        final XmlMapper xmlMapper = new XmlMapper();
        root = xmlMapper.readTree(Files.readString(path));
    }

    /**
     * Get String value of xml text value. Will return empty string if {@code xpath} is not exist or contains no value.
     */
    String getValue(final String xpath) {
        final JsonNode xmlElement = root.at(xpath);
        if (xmlElement.isMissingNode()) {
            logger.warn("xmlElement.isMissingNode() return true and getValue() with {} xpath will return empty string", xmlElement);
            return "";
        }
        return xmlElement.textValue();
    }
}
