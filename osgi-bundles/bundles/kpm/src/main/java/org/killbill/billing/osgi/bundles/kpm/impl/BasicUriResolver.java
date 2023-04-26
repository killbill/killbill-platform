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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.killbill.billing.osgi.bundles.kpm.UriResolver;

public class BasicUriResolver implements UriResolver {

    private final String baseUri;
    private final String username;
    private final String password;

    public BasicUriResolver(final String baseUri, final String username, final String password) {
        this.baseUri = baseUri;
        this.username = username;
        this.password = password;
    }

    @Override
    public String getBaseUri() {
        return baseUri;
    }

    @Override
    public AuthenticationMethod getAuthMethod() {
        return AuthenticationMethod.BASIC;
    }

    @Override
    public Map<String, String> getHeaders() {
        final String credentials = username + ":" + password;
        final String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return Map.of("Authorization", "Basic " + encoded);
    }
}
