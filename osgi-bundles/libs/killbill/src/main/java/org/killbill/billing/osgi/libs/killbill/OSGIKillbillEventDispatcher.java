/*
 * Copyright 2016 Groupon, Inc
 * Copyright 2016 The Billing Project, LLC
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

package org.killbill.billing.osgi.libs.killbill;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSGIKillbillEventDispatcher extends OSGIKillbillLibraryBase {

    private final Logger logger = LoggerFactory.getLogger(OSGIKillbillEventDispatcher.class);

    private static final String OBSERVABLE_SERVICE_NAME = "java.util.Observable";

    private final ServiceTracker<Observable, Observable> observableTracker;

    private final Map<Object, Observer> handlerToObserver;

    private final String symbolicName;

    public OSGIKillbillEventDispatcher(final BundleContext context) {
        symbolicName = context.getBundle().getSymbolicName();
        handlerToObserver = new HashMap<Object, Observer>();
        observableTracker = new ServiceTracker<Observable, Observable>(context, OBSERVABLE_SERVICE_NAME, null);
        observableTracker.open();
    }

    public void close() {
        if (observableTracker != null) {
            observableTracker.close();
        }
        handlerToObserver.clear();
    }

    public void registerEventHandlers(final OSGIHandlerMarker...handlerOfSomeTypes) {
        for (final OSGIHandlerMarker handlerOfSomeType : handlerOfSomeTypes) {
            registerEventHandler(handlerOfSomeType);
        }
    }


        //
    // We provide one registration method for both KillBill events and OSGI framework event since
    // the underlying mechanism is the same (classloader magic, and registerEventHandler call).
    //
    // The only difference is the handler method along with its events that needs to be called which are
    // implemented in the private methods handleKillbillEvent and handleOSGIStartEvent below
    //
    private void registerEventHandler(final OSGIHandlerMarker handlerOfSomeType) {
        final Observer observer = new Observer() {

            @Override
            public void update(final Observable o, final Object arg) {

                final ClassLoader initialContextClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(handlerOfSomeType.getClass().getClassLoader());
                try {
                    if (handlerOfSomeType instanceof OSGIKillbillEventHandler) {
                        handleKillbillEvent((OSGIKillbillEventHandler) handlerOfSomeType, arg);
                    } else if (handlerOfSomeType instanceof OSGIFrameworkEventHandler) {
                        handleOSGIStartEvent((OSGIFrameworkEventHandler) handlerOfSomeType, arg);
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(initialContextClassLoader);
                }
            }

            private void handleKillbillEvent(final OSGIKillbillEventHandler handler, final Object arg) {
                if (!(arg instanceof ExtBusEvent)) {
                    logger.debug("OSGIKillbillEventDispatcher unexpected event type " + (arg != null ? arg.getClass() : "null"));
                    return;
                }
                handler.handleKillbillEvent((ExtBusEvent) arg);
            }

            private void handleOSGIStartEvent(final OSGIFrameworkEventHandler handler, final Object arg) {
                if (!(arg instanceof Event)) {
                    logger.debug("OSGIFrameworkEventHandler unexpected event type " + (arg != null ? arg.getClass() : "null"));
                    return;
                }

                final String topic = ((Event) arg).getTopic();
                // Platform is up, all bundles/plugins have been started
                if ("org/killbill/billing/osgi/lifecycle/STARTED".equals(topic)) {
                    handler.started();
                } else if (("org/killbill/billing/osgi/plugin/START_PLUGIN".equals(topic) || "org/killbill/billing/osgi/plugin/RESTART_PLUGIN".equals(topic))) {

                    final String symbolicNameProperty = (String) ((Event) arg).getProperty("symbolicName");
                    // This specific plugin has been started/restarted
                    if (symbolicNameProperty != null && symbolicNameProperty.equals(symbolicName)) {
                        handler.started();
                    }
                }
            }
        };
        registerEventHandler(handlerOfSomeType, observer);
    }


    public void registerEventHandler(final OSGIHandlerMarker handler, final Observer observer) {
        withServiceTracker(observableTracker,
                           new APICallback<Void, Observable>(OBSERVABLE_SERVICE_NAME) {
                               @Override
                               public Void executeWithService(final Observable service) {
                                   handlerToObserver.put(handler, observer);
                                   service.addObserver(observer);
                                   return null;
                               }
                           });
    }

    public void unregisterEventHandler(final OSGIHandlerMarker handler) {
        withServiceTracker(observableTracker,
                           new APICallback<Void, Observable>(OBSERVABLE_SERVICE_NAME) {
                               @Override
                               public Void executeWithService(final Observable service) {
                                   final Observer observer = handlerToObserver.get(handler);
                                   if (observer != null) {
                                       service.deleteObserver(observer);
                                       handlerToObserver.remove(handler);
                                   }
                                   return null;
                               }
                           });
    }

    public void unregisterAllHandlers() {
        withServiceTracker(observableTracker,
                           new APICallback<Void, Observable>(OBSERVABLE_SERVICE_NAME) {
                               @Override
                               public Void executeWithService(final Observable service) {
                                   // Go through all known handlers (OSGIFrameworkEventHandler and OSGIKillbillEventHandler)
                                   // and remove them from the list of Observers
                                   for (final Object handler : handlerToObserver.keySet()) {
                                       final Observer observer = handlerToObserver.get(handler);
                                       if (observer != null) {
                                           service.deleteObserver(observer);
                                       }
                                   }
                                   handlerToObserver.clear();
                                   return null;
                               }
                           });
    }


    public interface OSGIHandlerMarker {
    }

    public interface OSGIKillbillEventHandler extends OSGIHandlerMarker {
        public void handleKillbillEvent(final ExtBusEvent killbillEvent);
    }

    public interface OSGIFrameworkEventHandler extends OSGIHandlerMarker {
        public void started();
    }
}
