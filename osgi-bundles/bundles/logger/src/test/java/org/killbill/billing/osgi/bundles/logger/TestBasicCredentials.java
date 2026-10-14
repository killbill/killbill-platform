/*
 * Copyright 2020-2026 Equinix, Inc
 * Copyright 2014-2026 The Billing Project, LLC
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

package org.killbill.billing.osgi.bundles.logger;

import java.util.Base64;
import java.util.Optional;

import org.jooby.Mutant;
import org.jooby.Request;
import org.killbill.billing.osgi.bundles.logger.BasicAuthFilter.BasicCredentials;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBasicCredentials {

    // -- decode() tests --

    @Test(groups = "fast")
    public void testDecodeValid() {
        final Optional<BasicCredentials> result = BasicCredentials.decode(basicHeader("admin", "password"));
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getUsername(), "admin");
        Assert.assertEquals(result.get().getPassword(), "password");
    }

    @Test(groups = "fast")
    public void testDecodePasswordContainsColon() {
        final Optional<BasicCredentials> result = BasicCredentials.decode(basicHeader("admin", "pass:word"));
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getUsername(), "admin");
        Assert.assertEquals(result.get().getPassword(), "pass:word");
    }

    @Test(groups = "fast")
    public void testDecodeNotBasicScheme() {
        Assert.assertEquals(BasicCredentials.decode("******"), Optional.empty());
    }

    @Test(groups = "fast")
    public void testDecodeSchemeIsCaseInsensitive() {
        // Auth-scheme tokens are matched case-insensitively (RFC 7235 section 2.1).
        final String encoded = Base64.getEncoder().encodeToString("admin:password".getBytes());

        final Optional<BasicCredentials> lowercase = BasicCredentials.decode("basic " + encoded);
        Assert.assertTrue(lowercase.isPresent());
        Assert.assertEquals(lowercase.get().getUsername(), "admin");
        Assert.assertEquals(lowercase.get().getPassword(), "password");

        final Optional<BasicCredentials> mixedCase = BasicCredentials.decode("BaSiC " + encoded);
        Assert.assertTrue(mixedCase.isPresent());
        Assert.assertEquals(mixedCase.get().getUsername(), "admin");
        Assert.assertEquals(mixedCase.get().getPassword(), "password");
    }

    @Test(groups = "fast")
    public void testDecodeInvalidBase64() {
        Assert.assertEquals(BasicCredentials.decode("Basic not-valid-base64!!"), Optional.empty());
    }

    @Test(groups = "fast")
    public void testDecodeMissingColon() {
        final String encoded = Base64.getEncoder().encodeToString("adminpassword".getBytes());
        Assert.assertEquals(BasicCredentials.decode("Basic " + encoded), Optional.empty());
    }

    @Test(groups = "fast")
    public void testDecodeEmptyUsername() {
        Assert.assertEquals(BasicCredentials.decode(basicHeader("", "password")), Optional.empty());
    }

    @Test(groups = "fast")
    public void testDecodeEmptyPassword() {
        Assert.assertEquals(BasicCredentials.decode(basicHeader("admin", "")), Optional.empty());
    }

    @Test(groups = "fast")
    public void testDecodeEmptyUsernameAndPassword() {
        Assert.assertEquals(BasicCredentials.decode(basicHeader("", "")), Optional.empty());
    }

    // -- fromRequest() tests --

    @Test(groups = "fast")
    public void testFromRequestNoAuthorizationHeader() {
        final Request request = mockRequestWithAuthorizationHeader(null);
        Assert.assertEquals(BasicCredentials.fromRequest(request), Optional.empty());
    }

    @Test(groups = "fast")
    public void testFromRequestValid() {
        final Request request = mockRequestWithAuthorizationHeader(basicHeader("admin", "password"));
        final Optional<BasicCredentials> result = BasicCredentials.fromRequest(request);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getUsername(), "admin");
        Assert.assertEquals(result.get().getPassword(), "password");
    }

    @Test(groups = "fast")
    public void testFromRequestMalformedHeader() {
        final Request request = mockRequestWithAuthorizationHeader("******");
        Assert.assertEquals(BasicCredentials.fromRequest(request), Optional.empty());
    }

    // -- helpers --

    private static String basicHeader(final String username, final String password) {
        final String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        return "Basic " + encoded;
    }

    private static Request mockRequestWithAuthorizationHeader(final String value) {
        final Request request = Mockito.mock(Request.class);
        final Mutant mutant = Mockito.mock(Mutant.class);
        Mockito.when(mutant.toOptional()).thenReturn(value == null ? Optional.empty() : Optional.of(value));
        Mockito.when(request.header("Authorization")).thenReturn(mutant);
        return request;
    }
}
