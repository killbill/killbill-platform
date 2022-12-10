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

package org.killbill.billing.lifecycle;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.killbill.billing.lifecycle.api.Lifecycle;
import org.killbill.billing.platform.api.KillbillService;
import org.killbill.billing.platform.api.LifecycleHandlerType;
import org.killbill.billing.platform.api.LifecycleHandlerType.LifecycleLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.ConfigurationException;

import com.google.inject.Injector;
import com.google.inject.ProvisionException;

public class DefaultLifecycle implements Lifecycle {

    private static final Logger log = LoggerFactory.getLogger(DefaultLifecycle.class);

    // See https://github.com/killbill/killbill-commons/issues/143
    private final Map<LifecycleLevel, SortedSet<LifecycleHandler<? extends KillbillService>>> handlersByLevel;

    @Inject
    public DefaultLifecycle(final Injector injector) {
        this();
        final ServiceFinder<KillbillService> serviceFinder = new ServiceFinder<>(DefaultLifecycle.class.getClassLoader(), KillbillService.class.getName());
        init(serviceFinder, injector);
    }

    // For testing
    public DefaultLifecycle(final Iterable<? extends KillbillService> services) {
        this();
        init(services);
    }


    private DefaultLifecycle() {
        this.handlersByLevel = new ConcurrentHashMap<>();
    }

    @Override
    public void fireStartupSequencePriorEventRegistration() {
        fireSequence(LifecycleHandlerType.LifecycleLevel.Sequence.STARTUP_PRE_EVENT_REGISTRATION);
    }

    @Override
    public void fireStartupSequencePostEventRegistration() {
        fireSequence(LifecycleHandlerType.LifecycleLevel.Sequence.STARTUP_POST_EVENT_REGISTRATION);
    }

    @Override
    public void fireShutdownSequencePriorEventUnRegistration() {
        fireSequence(LifecycleHandlerType.LifecycleLevel.Sequence.SHUTDOWN_PRE_EVENT_UNREGISTRATION);
    }

    @Override
    public void fireShutdownSequencePostEventUnRegistration() {
        fireSequence(LifecycleHandlerType.LifecycleLevel.Sequence.SHUTDOWN_POST_EVENT_UNREGISTRATION);
    }

    private Set<? extends KillbillService> findServices(final Set<Class<? extends KillbillService>> services, final Injector injector) {
        final Set<KillbillService> result = new HashSet<>();
        for (final Class<? extends KillbillService> cur : services) {
            log.debug("Found service {}", cur.getName());
            try {
                final KillbillService instance = injector.getInstance(cur);
                log.debug("got instance {}", instance.getName());
                result.add(instance);
            } catch (final ConfigurationException e) {
                if (!cur.getSimpleName().startsWith("Test")) {
                    // The service has not implementation - this may be fine (e.g. tests), don't log the full stack trace
                    logWarn("Failed to inject " + cur.getName(), null);
                }
            } catch (final ProvisionException e) {
                if (!cur.getSimpleName().startsWith("Test")) {
                    logWarn("Failed to inject " + cur.getName(), e);
                }
            }

        }
        return result;
    }

    private void init(final ServiceFinder<KillbillService> serviceFinder, final Injector injector) {
        init(serviceFinder.getServices(), injector);
    }

    private void init(final Set<Class<? extends KillbillService>> servicesClasses, final Injector injector) {
        final Set<? extends KillbillService> services = findServices(servicesClasses, injector);
        init(services);
    }

    private void init(final Iterable<? extends KillbillService> services) {
        for (final KillbillService service : services) {
            final Map<LifecycleLevel, SortedSet<LifecycleHandler<? extends KillbillService>>> values = findAllHandlers(service);
            for (final Entry<LifecycleLevel, SortedSet<LifecycleHandler<? extends KillbillService>>> entry : values.entrySet()) {
                if (handlersByLevel.get(entry.getKey()) != null) {
                    handlersByLevel.get(entry.getKey()).addAll(entry.getValue());
                } else {
                    handlersByLevel.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void fireSequence(final LifecycleHandlerType.LifecycleLevel.Sequence seq) {
        final List<LifecycleHandlerType.LifecycleLevel> levels = LifecycleHandlerType.LifecycleLevel.getLevelsForSequence(seq);
        for (final LifecycleHandlerType.LifecycleLevel cur : levels) {
            doFireStage(cur);
        }
    }

    private void doFireStage(final LifecycleHandlerType.LifecycleLevel level) {
        log.info("Killbill lifecycle firing stage {}", level);
        final Set<LifecycleHandler<? extends KillbillService>> handlers = handlersByLevel.getOrDefault(level, new TreeSet<>());
        for (final LifecycleHandler<? extends KillbillService> cur : handlers) {

            try {
                final Method method = cur.getMethod();
                final KillbillService target = cur.getTarget();
                log.info("Killbill lifecycle calling handler {} for service {}", cur.getMethod().getName(), target.getName());
                method.invoke(target);
            } catch (final Exception e) {
                logWarn("Killbill lifecycle failed to invoke lifecycle handler", e);
            }
        }

    }

    // Used to disable valid injection failure from unit tests
    protected void logWarn(final String msg, @Nullable final Exception e) {
        if (e == null) {
            log.warn(msg);
        } else {
            log.warn(msg, e);
        }
    }

    private Map<LifecycleHandlerType.LifecycleLevel, SortedSet<LifecycleHandler<? extends KillbillService>>> findAllHandlers(final KillbillService service) {
        final Map<LifecycleHandlerType.LifecycleLevel, SortedSet<LifecycleHandler<? extends KillbillService>>> methodsInService = new HashMap<>();
        final Class<? extends KillbillService> clazz = service.getClass();
        for (final Method method : clazz.getMethods()) {
            final LifecycleHandlerType annotation = method.getAnnotation(LifecycleHandlerType.class);
            if (annotation != null) {
                final LifecycleHandlerType.LifecycleLevel level = annotation.value();
                final LifecycleHandler<? extends KillbillService> handler = new LifecycleHandler<>(service, method);
                if (methodsInService.get(level) != null) {
                    methodsInService.get(level).add(handler);
                } else {
                    final SortedSet<LifecycleHandler<? extends KillbillService>> handlers = new TreeSet<>();
                    handlers.add(handler);
                    methodsInService.put(level, handlers);
                }
            }
        }
        return methodsInService;
    }

    static final class LifecycleHandler<T extends KillbillService> implements Comparable<LifecycleHandler<?>> {

        private final T target;
        private final Method method;

        public LifecycleHandler(final T target, final Method method) {
            this.target = target;
            this.method = method;
        }

        public T getTarget() {
            return target;
        }

        public Method getMethod() {
            return method;
        }


        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final LifecycleHandler<?> that = (LifecycleHandler<?>) o;
            return Objects.equals(target, that.target) &&
                   Objects.equals(method, that.method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(target, method);
        }

        @Override
        public int compareTo(final LifecycleHandler o) {
            if (target.getRegistrationOrdering() < o.getTarget().getRegistrationOrdering()) {
                return -1;
            } else if (target.getRegistrationOrdering() > o.getTarget().getRegistrationOrdering()) {
                return 1;
            } else {
                return Integer.compare(target.hashCode(), o.hashCode());
            }
        }
    }

    Map<LifecycleLevel, SortedSet<LifecycleHandler<? extends KillbillService>>> getHandlersByLevel() {
        return handlersByLevel;
    }
}
