/*
 * Copyright 2010-2013 Ning, Inc.
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

package org.killbill.billing.osgi;

import java.lang.reflect.Field;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.inject.Inject;
import javax.inject.Named;

import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.bus.api.PersistentBus;
import org.killbill.bus.api.PersistentBus.EventBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

public class KillbillEventObservable extends Observable {

    private final Logger logger = LoggerFactory.getLogger(KillbillEventObservable.class);

    private final PersistentBus externalBus;

    @Inject
    public KillbillEventObservable(@Named("externalBus") final PersistentBus externalBus) {
        this.externalBus = externalBus;
    }

    public void register() throws EventBusException {
        externalBus.register(this);
    }

    public void unregister() throws EventBusException {
        deleteObservers();
        if (externalBus != null) {
            externalBus.unregister(this);
        }
    }

    //
    // Override notifyObservers from Observable to prevent from having to
    // call setChanged and then notifyObservers, which are not atomic
    // and can lead to losing events.
    //
    @Override
    public void notifyObservers(Object arg) {
        final Vector obsCopy = getDeclaredField("obs");
        final Object[] arrLocal = obsCopy.toArray();
        for (int i = arrLocal.length - 1; i >= 0; i--) {
            ((Observer) arrLocal[i]).update(this, arg);
        }
    }

    @AllowConcurrentEvents
    @Subscribe
    public void handleKillbillEvent(final ExtBusEvent event) {
        logger.debug("Received external event " + event.toString());
        setChangedAndNotifyObservers(event);
    }

    public void setChangedAndNotifyObservers(final Object event) {
        setChanged();
        notifyObservers(event);
    }

    //
    // Ugly hack to access private field 'obs'
    //
    @SuppressWarnings("unchecked")
    private <T> T getDeclaredField(final String fieldName) {
        try {
            final Field f = Observable.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(this);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to retrieve private field from Observable class " + fieldName, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to retrieve private field from Observable class " + fieldName, e);
        }
    }
}
