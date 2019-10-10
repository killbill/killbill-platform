/*
 * Copyright 2014-2019 Groupon, Inc
 * Copyright 2014-2019 The Billing Project, LLC
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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jooby.Result;
import org.jooby.Results;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;

@Singleton
// Handle /plugins/killbill-kpm/plugins (for KPM UI)
@Path("/plugins")
public class PluginsResource {

    private final KPMWrapper kpmWrapper;

    @Inject
    public PluginsResource(final KPMWrapper kpmWrapper) {
        this.kpmWrapper = kpmWrapper;
    }

    @GET
    public Result getAvailablePlugins(final String kbVersion, final Boolean latest) {
        return Results.ok(kpmWrapper.getAvailablePlugins(kbVersion, latest));
    }
}
