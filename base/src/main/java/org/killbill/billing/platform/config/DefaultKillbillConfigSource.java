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

package org.killbill.billing.platform.config;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nullable;

import org.killbill.billing.osgi.api.OSGIConfigProperties;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.xmlloader.UriAccessor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class DefaultKillbillConfigSource implements KillbillConfigSource, OSGIConfigProperties {

    private final PropertyKillbillConfigSource propertySource;
    private final KillbillConfigSourceChain chain;

    public DefaultKillbillConfigSource() throws IOException, URISyntaxException {
        this((String) null);
    }

    public DefaultKillbillConfigSource(final Map<String, String> extraDefaultProperties) throws IOException, URISyntaxException {
        this(null, extraDefaultProperties);
    }

    public DefaultKillbillConfigSource(@Nullable final String file) throws URISyntaxException, IOException {
        this(file, ImmutableMap.<String, String>of());
    }

    public DefaultKillbillConfigSource(@Nullable final String file, final Map<String, String> extraDefaultProperties) throws URISyntaxException, IOException {
        this.propertySource = new PropertyKillbillConfigSource(file, extraDefaultProperties);
        this.chain = new KillbillConfigSourceChain(ImmutableList.of(new EnvKillbillConfigSource(), propertySource));
    }

    @Override
    public Properties getProperties() {
        return propertySource.getProperties();
    }

    @Override
    public String getString(final String propertyName) {
        return chain.getString(propertyName);
    }

    @Override
    public boolean hasProperty(final String propertyName) {
        return chain.hasProperty(propertyName);
    }
}
