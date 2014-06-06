/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
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

import com.google.common.base.Supplier;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import org.killbill.billing.lifecycle.api.Lifecycle;
import org.killbill.billing.platform.api.KillbillService;
import org.killbill.billing.platform.api.LifecycleHandlerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class DefaultLifecycle implements Lifecycle {

    private static final Logger log = LoggerFactory.getLogger(DefaultLifecycle.class);
    private final SetMultimap<LifecycleHandlerType.LifecycleLevel, LifecycleHandler<? extends KillbillService>> handlersByLevel;

    @Inject
    public DefaultLifecycle(final Injector injector) {
        this();
        final ServiceFinder serviceFinder = new ServiceFinder(DefaultLifecycle.class.getClassLoader());
        init(serviceFinder, injector);
    }

    // For testing
    public DefaultLifecycle(final Set<? extends KillbillService> services) {
        this();
        init(services);
    }

    private DefaultLifecycle() {
        this.handlersByLevel = Multimaps.newSetMultimap(new ConcurrentHashMap<LifecycleHandlerType.LifecycleLevel, Collection<LifecycleHandler<? extends KillbillService>>>(),

                                                        new Supplier<Set<LifecycleHandler<? extends KillbillService>>>() {
                                                            @Override
                                                            public Set<LifecycleHandler<? extends KillbillService>> get() {
                                                                return new CopyOnWriteArraySet<LifecycleHandler<? extends KillbillService>>();
                                                            }
                                                        });
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

    private Set<? extends KillbillService> findServices(Set<Class<? extends KillbillService>> services, Injector injector) {
        final Set<KillbillService> result = new HashSet<KillbillService>();
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

    private void init(final ServiceFinder serviceFinder, final Injector injector) {
        init(serviceFinder.getServices(), injector);
    }

    private void init(Set<Class<? extends KillbillService>> servicesClasses, final Injector injector) {
        final Set<? extends KillbillService> services = findServices(servicesClasses, injector);
        init(services);
    }

    private void init(Set<? extends KillbillService> services) {
        for (final KillbillService service : services) {
            handlersByLevel.putAll(findAllHandlers(service));
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
        final Set<LifecycleHandler<? extends KillbillService>> handlers = handlersByLevel.get(level);
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

    private Multimap<LifecycleHandlerType.LifecycleLevel, LifecycleHandler<? extends KillbillService>> findAllHandlers(final KillbillService service) {
        final Multimap<LifecycleHandlerType.LifecycleLevel, LifecycleHandler<? extends KillbillService>> methodsInService = HashMultimap.create();
        final Class<? extends KillbillService> clazz = service.getClass();
        for (final Method method : clazz.getMethods()) {
            final LifecycleHandlerType annotation = method.getAnnotation(LifecycleHandlerType.class);
            if (annotation != null) {
                final LifecycleHandlerType.LifecycleLevel level = annotation.value();
                final LifecycleHandler<? extends KillbillService> handler = new LifecycleHandler<KillbillService>(service, method);
                methodsInService.put(level, handler);
            }
        }
        return methodsInService;
    }

    private final class LifecycleHandler<T> {

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
    }
}
