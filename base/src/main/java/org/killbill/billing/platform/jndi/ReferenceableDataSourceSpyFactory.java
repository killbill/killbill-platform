/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

package org.killbill.billing.platform.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.killbill.commons.utils.Preconditions;

public class ReferenceableDataSourceSpyFactory implements ObjectFactory {

    public static final String DATA_SOURCE_ID = "dataSourceId";

    @Override
    public Object getObjectInstance(final Object obj, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
        if (obj instanceof Reference) {
            final Reference reference = (Reference) obj;

            final RefAddr dataSourceIdAddr = reference.get(DATA_SOURCE_ID);
            Preconditions.checkNotNull(dataSourceIdAddr);

            final String dataSourceId = (String) dataSourceIdAddr.getContent();
            Preconditions.checkNotNull(dataSourceId);

            return new ReferenceableDataSourceSpy(dataSourceId);
        }

        return null;
    }
}
