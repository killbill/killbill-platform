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

package org.killbill.billing.osgi.bundles.kpm;

public class KPMPluginOperationException extends KPMPluginException {

    private enum Operation {
        INSTALL, UNINSTALL;
    }

    private final Operation operation;

    public KPMPluginOperationException(final Throwable e, final Operation operation) {
        super(e);
        this.operation = operation;
    }

    public static KPMPluginOperationException newInstallException(final Throwable e) {
        return new KPMPluginOperationException(e, Operation.INSTALL);
    }

    public static KPMPluginOperationException newUninstallException(final Throwable e) {
        return new KPMPluginOperationException(e, Operation.UNINSTALL);
    }

    @Override
    public String toString() {
        return super.toString() + " when doing " + operation;
    }
}
