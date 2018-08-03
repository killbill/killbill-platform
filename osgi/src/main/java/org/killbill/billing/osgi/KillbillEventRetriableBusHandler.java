/*
 * Copyright 2014-2017 Groupon, Inc
 * Copyright 2014-2017 The Billing Project, LLC
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

import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.osgi.api.KillbillEventRetriableBusHandlerService;
import org.killbill.billing.platform.api.LifecycleHandlerType;
import org.killbill.billing.platform.api.LifecycleHandlerType.LifecycleLevel;
import org.killbill.bus.api.BusEvent;
import org.killbill.bus.api.PersistentBus;
import org.killbill.bus.api.PersistentBus.EventBusException;
import org.killbill.clock.Clock;
import org.killbill.notificationq.api.NotificationQueueService;
import org.killbill.notificationq.api.NotificationQueueService.NoSuchNotificationQueue;
import org.killbill.queue.QueueObjectMapper;
import org.killbill.queue.retry.RetryableService;
import org.killbill.queue.retry.RetryableSubscriber;
import org.killbill.queue.retry.RetryableSubscriber.SubscriberAction;
import org.killbill.queue.retry.RetryableSubscriber.SubscriberQueueHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

// Needs to be injected for the lifecycle logic
public class KillbillEventRetriableBusHandler extends RetryableService implements KillbillEventRetriableBusHandlerService {

    private final Logger logger = LoggerFactory.getLogger(KillbillEventRetriableBusHandler.class);

    private final PersistentBus externalBus;
    private final KillbillEventObservable killbillEventObservable;
    private final RetryableSubscriber retryableSubscriber;
    private final SubscriberQueueHandler subscriberQueueHandler = new SubscriberQueueHandler();

    @Inject
    public KillbillEventRetriableBusHandler(@Named("externalBus") final PersistentBus externalBus,
                                            final KillbillEventObservable killbillEventObservable,
                                            final NotificationQueueService notificationQueueService,
                                            final Clock clock) {
        super(notificationQueueService);
        this.externalBus = externalBus;
        this.killbillEventObservable = killbillEventObservable;
        subscriberQueueHandler.subscribe(OSGIBusEvent.class,
                                         new SubscriberAction<OSGIBusEvent>() {
                                             @Override
                                             public void run(final OSGIBusEvent osgiBusEvent) {
                                                 final ExtBusEvent extBusEvent = osgiBusEvent.getExtBusEvent();
                                                 logger.debug("Received external event " + extBusEvent.toString());
                                                 killbillEventObservable.setChangedAndNotifyObservers(extBusEvent);
                                             }
                                         });
        this.retryableSubscriber = new RetryableSubscriber(clock, this, subscriberQueueHandler);
    }

    public void register() throws EventBusException {
        externalBus.register(this);
    }

    public void unregister() throws EventBusException {
        killbillEventObservable.deleteObservers();
        if (externalBus != null) {
            externalBus.unregister(this);
        }
    }

    @Override
    public String getName() {
        return KILLBILL_SERVICES.RETRIABLE_BUS_HANDLER_SERVICE.getServiceName();
    }

    @Override
    public int getRegistrationOrdering() {
        return KILLBILL_SERVICES.RETRIABLE_BUS_HANDLER_SERVICE.getRegistrationOrdering();
    }

    @LifecycleHandlerType(LifecycleLevel.INIT_SERVICE)
    public void initialize() {
        super.initialize("extBusEvent-listener", subscriberQueueHandler);
    }

    @LifecycleHandlerType(LifecycleLevel.START_SERVICE)
    public void start() {
        super.start();
    }

    @LifecycleHandlerType(LifecycleLevel.STOP_SERVICE)
    public void stop() throws NoSuchNotificationQueue {
        super.stop();
    }

    @AllowConcurrentEvents
    @Subscribe
    public void handleKillbillEvent(final ExtBusEvent extBusEvent) {
        final BusEvent event = new OSGIBusEvent(extBusEvent, extBusEvent.getClass());
        retryableSubscriber.handleEvent(event);
    }

    @JsonDeserialize(using = OSGIBusEventDeserializer.class)
    protected static class OSGIBusEvent implements BusEvent {

        private final ExtBusEvent extBusEvent;
        private final Class extBusEventClass;

        @JsonCreator
        public OSGIBusEvent(@JsonProperty("extBusEvent") final ExtBusEvent extBusEvent,
                            @JsonProperty("extBusEventClass") final Class extBusEventClass) {
            this.extBusEvent = extBusEvent;
            this.extBusEventClass = extBusEventClass;
        }

        public ExtBusEvent getExtBusEvent() {
            return extBusEvent;
        }

        public Class getExtBusEventClass() {
            return extBusEventClass;
        }

        @Override
        public Long getSearchKey1() {
            final UUID accountId = extBusEvent.getAccountId();
            return accountId == null ? null : accountId.getMostSignificantBits() & Long.MAX_VALUE;
        }

        @Override
        public Long getSearchKey2() {
            final UUID tenantId = extBusEvent.getTenantId();
            return tenantId == null ? null : tenantId.getMostSignificantBits() & Long.MAX_VALUE;
        }

        @Override
        public UUID getUserToken() {
            return extBusEvent.getUserToken();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("OSGIBusEvent{");
            sb.append("extBusEvent=").append(extBusEvent);
            sb.append(", extBusEventClass=").append(extBusEventClass);
            sb.append('}');
            return sb.toString();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final OSGIBusEvent event = (OSGIBusEvent) o;

            if (extBusEvent != null ? !extBusEvent.equals(event.extBusEvent) : event.extBusEvent != null) {
                return false;
            }
            return extBusEventClass != null ? extBusEventClass.equals(event.extBusEventClass) : event.extBusEventClass == null;
        }

        @Override
        public int hashCode() {
            int result = extBusEvent != null ? extBusEvent.hashCode() : 0;
            result = 31 * result + (extBusEventClass != null ? extBusEventClass.hashCode() : 0);
            return result;
        }
    }

    protected static class OSGIBusEventDeserializer extends JsonDeserializer<OSGIBusEvent> {

        private static final ObjectMapper objectMapper = QueueObjectMapper.get();

        @Override
        public OSGIBusEvent deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            final JsonNode node = p.getCodec().readTree(p);

            final Class<ExtBusEvent> extBusEventClass;
            try {
                extBusEventClass = (Class<ExtBusEvent>) Class.forName(node.get("extBusEventClass").textValue());
            } catch (final ClassNotFoundException e) {
                throw new IOException(e);
            }

            return new OSGIBusEvent(objectMapper.treeToValue(node.get("extBusEvent"), extBusEventClass), extBusEventClass);
        }
    }
}
