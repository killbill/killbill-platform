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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.spi.NamingManager;

import org.killbill.commons.utils.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JNDIManager {

    private static final Logger logger = LoggerFactory.getLogger(JNDIManager.class);

    private static final String REGISTRY_CONTEXT_FACTORY = "com.sun.jndi.rmi.registry.RegistryContextFactory";

    private final int port;

    public JNDIManager() throws RemoteException {
        this(Registry.REGISTRY_PORT);
    }

    public JNDIManager(final int port) throws RemoteException {
        this.port = port;

        try {
            LocateRegistry.createRegistry(port);
        } catch (final ExportException ignored) {
            // Already running
        }

        // Set these properties globally, so individual plugins don't have to worry about it
        System.setProperty(Context.PROVIDER_URL, "rmi://127.0.0.1:" + port);
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, REGISTRY_CONTEXT_FACTORY);
    }

    public void export(final String name, final Object object) {
        Preconditions.checkArgument(object instanceof Remote || object instanceof Reference || object instanceof Referenceable,
                                    "object to bind must be Remote, Reference, or Referenceable, not " + object.getClass());
        doExport(name, object);
    }

    public void unExport(final String name) {
        Context context = null;

        try {
            context = getContext();
            context.removeFromEnvironment(name);
        } catch (final NamingException e) {
            logger.warn("Error un-exporting " + name, e);
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (final NamingException e) {
                    logger.warn("Error closing context while un-exporting " + name, e);
                }
            }
        }
    }

    public Object lookup(final String name) {
        Context context = null;

        try {
            context = getContext();
            final Object obj = context.lookup(name);

            if (obj instanceof Reference) {
                try {
                    return NamingManager.getObjectInstance(obj, null, null, null);
                } catch (final Exception e) {
                    logger.warn("Error dereferencing " + name, e);
                }
            }

            return obj;
        } catch (final NamingException e) {
            logger.warn("Error looking up " + name, e);
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (final NamingException e) {
                    logger.warn("Error closing context while looking up " + name, e);
                }
            }
        }

        return null;
    }

    private void doExport(final String name, final Object object) {
        Context context = null;

        try {
            context = getContext();
            context.rebind(name, object);
        } catch (final NamingException e) {
            logger.warn("Error exporting " + name, e);
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (final NamingException e) {
                    logger.warn("Error closing context while exporting " + name, e);
                }
            }
        }
    }

    private Context getContext() throws NamingException {
        final Hashtable<String, String> environment = new Hashtable<String, String>();
        environment.put(Context.PROVIDER_URL, "rmi://127.0.0.1:" + port);
        environment.put(Context.INITIAL_CONTEXT_FACTORY, REGISTRY_CONTEXT_FACTORY);

        return new InitialContext(environment);
    }
}
