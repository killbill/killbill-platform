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

import org.killbill.commons.utils.annotation.VisibleForTesting;

public final class KpmPropertiesValidator {

    private final KpmProperties kpmProperties;

    public KpmPropertiesValidator(final KpmProperties kpmProperties) {
        this.kpmProperties = kpmProperties;
    }

    public void validate() throws KPMPluginException {
        this.validateNexusUrl();
        this.validateNexusRepository();
    }

    @VisibleForTesting
    void validateNexusUrl() throws KPMPluginException {
        if (kpmProperties.getNexusUrl().endsWith("/")) {
            throw new KPMPluginException("nexusUrl should not end with a trailing slash");
        }
    }

    @VisibleForTesting
    void validateNexusRepository() throws KPMPluginException {
        final String toValidate = kpmProperties.getNexusRepository();
        if (!toValidate.startsWith("/") || toValidate.endsWith("/")) {
            throw new KPMPluginException("nexusRepository should start with a slash and not end with a trailing slash");
        }
    }
}
