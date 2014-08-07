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

package org.killbill.billing.platform.jndi.utils;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.spi.ObjectFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceIndirector {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceIndirector.class);

    public ReferenceSerialized indirectForm(final Object orig) throws Exception {
        final Reference ref = ((Referenceable) orig).getReference();
        return new ReferenceSerialized(ref);
    }

    public static class ReferenceSerialized implements Serializable {

        private final Reference reference;

        ReferenceSerialized(final Reference reference) {
            this.reference = reference;
        }

        public Object getObject() throws ClassNotFoundException, IOException {
            try {
                return referenceToObject(reference);
            } catch (final NamingException e) {
                logger.warn("Failed to acquire the Context necessary to lookup an Object.", e);
                throw new InvalidObjectException("Failed to acquire the Context necessary to lookup an Object: " + e.toString());
            }
        }

        public static Object referenceToObject(final Reference ref) throws NamingException {
            try {
                final String fClassName = ref.getFactoryClassName();
                final String fClassLocation = ref.getFactoryClassLocation();

                final ClassLoader cl;
                if (fClassLocation == null) {
                    cl = ClassLoader.getSystemClassLoader();
                } else {
                    final URL u = new URL(fClassLocation);
                    cl = new URLClassLoader(new URL[]{u}, ClassLoader.getSystemClassLoader());
                }

                final Class fClass = Class.forName(fClassName, true, cl);
                final ObjectFactory of = (ObjectFactory) fClass.newInstance();
                return of.getObjectInstance(ref, null, null, null);
            } catch (final Exception e) {
                final NamingException ne = new NamingException("Could not resolve Reference to Object!");
                ne.setRootCause(e);
                throw ne;
            }
        }
    }
}
