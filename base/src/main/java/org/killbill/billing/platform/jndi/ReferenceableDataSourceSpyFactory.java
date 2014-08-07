/*
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

package org.killbill.billing.platform.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;

import org.killbill.billing.platform.jndi.utils.JavaBeanObjectFactory;

public class ReferenceableDataSourceSpyFactory implements ObjectFactory {

    @Override
    public Object getObjectInstance(final Object obj, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
        if (obj instanceof Reference) {
            final ObjectFactory factory = new JavaBeanObjectFactory();
            final DataSource dataSource = (DataSource) factory.getObjectInstance(obj, name, nameCtx, environment);
            return new ReferenceableDataSourceSpy<DataSource>(dataSource);
        }

        return null;
    }
}
