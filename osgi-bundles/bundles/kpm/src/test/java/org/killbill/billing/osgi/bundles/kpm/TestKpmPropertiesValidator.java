/*
 * Copyright 2020-2024 Equinix, Inc
 * Copyright 2014-2024 The Billing Project, LLC
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

package org.killbill.billing.osgi.bundles.kpm;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestKpmPropertiesValidator {

    @Test(groups = "fast")
    public void testValidateNexusUrl_noTrailingSlash() throws KPMPluginException {
        // Valid
        final KpmProperties kpmProperties = Mockito.mock(KpmProperties.class);
        Mockito.when(kpmProperties.getNexusUrl()).thenReturn("http://example.com");
        KpmPropertiesValidator validator = new KpmPropertiesValidator(kpmProperties);
        validator.validateNexusUrl();

        // Invalid: end with "/"
        Mockito.when(kpmProperties.getNexusUrl()).thenReturn("http://example.com/");
        validator = new KpmPropertiesValidator(kpmProperties);
        try {
            validator.validateNexusUrl();
        } catch (final KPMPluginException e) {
            Assert.assertEquals(e.getMessage(), "nexusUrl should not end with a trailing slash");
        }
    }

    @Test(groups = "fast")
    public void testValidateNexusRepository() throws KPMPluginException {
        final KpmProperties kpmProperties = Mockito.mock(KpmProperties.class);

        // Valid
        Mockito.when(kpmProperties.getNexusRepository()).thenReturn("/valid/nexus/repository/location");
        KpmPropertiesValidator validator = new KpmPropertiesValidator(kpmProperties);
        validator.validateNexusRepository();

        // Should start with "/"
        Mockito.when(kpmProperties.getNexusRepository()).thenReturn("some/invalid/repository");
        validator = new KpmPropertiesValidator(kpmProperties);
        try {
            validator.validateNexusRepository();
        } catch (final KPMPluginException e) {
            Assert.assertEquals(e.getMessage(), "nexusRepository should start with a slash and not end with a trailing slash");
        }

        // Should never end with "/"
        Mockito.when(kpmProperties.getNexusRepository()).thenReturn("/some/other/invalid/");
        validator = new KpmPropertiesValidator(kpmProperties);
        try {
            validator.validateNexusRepository();
        } catch (final KPMPluginException e) {
            Assert.assertEquals(e.getMessage(), "nexusRepository should start with a slash and not end with a trailing slash");
        }
    }
}
