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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.killbill.commons.profiling.Profiling;
import org.killbill.commons.profiling.Profiling.WithProfilingCallback;
import org.killbill.commons.profiling.ProfilingFeature.ProfilingFeatureType;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class ContextClassLoaderHelper {

    private static final Joiner DOT_JOINER = Joiner.on(".");

    /*
      http://impalablog.blogspot.com/2008/10/using-threads-callcontext-class-loader-in.html:

      "Many existing java libraries are designed to run inside a container (J2EE container, Applet container etc).
      Such containers explicitly define execution boundaries between the various components running within the container.
      The container controls the execution boundaries and knows when a boundary is being crossed from one component to the next.

      This level of boundary control allows a container to switch the callcontext of a thread when a component boundary is crossed.
      Typically when a container detects a callcontext switch it will set the callcontext class loader on the thread to a class loader associated with the component which is being entered.
      When the component is exited then the container will switch the callcontext class loader back to the previous callcontext class loader.

      The OSGi Framework specification does not define what the callcontext class loader should be set to and does not define when it should be switched.
      Part of the problem is the Framework is not always aware of when a component boundary is crossed."

      => So our current implementation is to proxy all calls from Killbill to OSGI registered services, and set/unset classloader before/after entering the call

    */

    public static <T> T getWrappedServiceWithCorrectContextClassLoader(final T service, final Class<T> serviceType, final String serviceName, @Nullable final MetricRegistry metricRegistry) {

        final Class<T> serviceClass = (Class<T>) service.getClass();
        final List<Class> allServiceInterfaces = getAllInterfaces(serviceClass);
        final Class[] serviceClassInterfaces = allServiceInterfaces.toArray(new Class[allServiceInterfaces.size()]);

        final InvocationHandler handler = new ClassLoaderInvocationHandler<T>(service, serviceName, serviceType, metricRegistry);
        final T wrappedService = (T) Proxy.newProxyInstance(serviceClass.getClassLoader(),
                                                            serviceClassInterfaces,
                                                            handler);
        return wrappedService;
    }

    // From apache-commons
    private static List getAllInterfaces(Class cls) {
        if (cls == null) {
            return null;
        }
        final List list = new ArrayList();
        while (cls != null) {
            final Class[] interfaces = cls.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                if (list.contains(interfaces[i]) == false) {
                    list.add(interfaces[i]);
                }
                final List superInterfaces = getAllInterfaces(interfaces[i]);
                for (final Iterator it = superInterfaces.iterator(); it.hasNext(); ) {
                    final Class intface = (Class) it.next();
                    if (list.contains(intface) == false) {
                        list.add(intface);
                    }
                }
            }
            cls = cls.getSuperclass();
        }
        return list;
    }

    private static class ClassLoaderInvocationHandler<T> implements InvocationHandler {

        private final String serviceName;
        private final T service;
        private final Class<?> serviceClass;
        private final String serviceInterfaceName;
        private final MetricRegistry metricRegistry;

        private LoadingCache<String, Timer> timerMetricCache;
        private LoadingCache<String, Meter> errorMetricCache;

        public ClassLoaderInvocationHandler(final T service,
                                            final String serviceName,
                                            final Class<T> serviceInterface,
                                            final MetricRegistry metricRegistry) {
            this.service = service;
            this.serviceName = serviceName;
            this.metricRegistry = metricRegistry;

            this.serviceClass = service.getClass();
            this.serviceInterfaceName = serviceInterface.getSimpleName();

            initializeMetricCaches();
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final ClassLoader initialContextClassLoader = Thread.currentThread().getContextClassLoader();
            final Context timerContext = timer(method).time();
            try {
                Thread.currentThread().setContextClassLoader(serviceClass.getClassLoader());
                final String methodName = method.getName();

                final Profiling<Object, Throwable> prof = new Profiling<Object, Throwable>();
                final String profilingId = serviceInterfaceName + "." + methodName;
                return prof.executeWithProfiling(ProfilingFeatureType.PLUGIN, profilingId, new WithProfilingCallback() {
                    @Override
                    public Object execute() throws Throwable {
                        return method.invoke(service, args);
                    }
                });
            } catch (final InvocationTargetException e) {
                final Meter errors = errorMeter(method);
                errors.mark();
                if (e.getCause() != null) {
                    throw e.getCause();
                } else {
                    throw new RuntimeException(e);
                }
            } finally {
                timerContext.stop();
                Thread.currentThread().setContextClassLoader(initialContextClassLoader);
            }
        }

        private Timer timer(final Method method) {
            return timerMetricCache.getUnchecked(method.getName());
        }

        private Meter errorMeter(final Method method) {
            return errorMetricCache.getUnchecked(method.getName());
        }

        private void initializeMetricCaches() {
            timerMetricCache = CacheBuilder.newBuilder().build(new CacheLoader<String, Timer>() {
                @Override
                public Timer load(final String methodName) {
                    final String timerMetricName = DOT_JOINER.join("killbill-service",
                                                                   "kb_plugin_latency",
                                                                   serviceName,
                                                                   serviceInterfaceName,
                                                                   methodName);

                    return metricRegistry.timer(timerMetricName);
                }
            });
            errorMetricCache = CacheBuilder.newBuilder().build(new CacheLoader<String, Meter>() {
                @Override
                public Meter load(final String methodName) {
                    final String counterMetricName = DOT_JOINER.join("killbill-service",
                                                                     "kb_plugin_errors",
                                                                     serviceName,
                                                                     serviceInterfaceName,
                                                                     methodName);

                    return metricRegistry.meter(counterMetricName);
                }
            });
        }
    }
}
