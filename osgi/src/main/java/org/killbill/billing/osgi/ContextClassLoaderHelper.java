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
import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.commons.profiling.Profiling;
import org.killbill.commons.profiling.Profiling.WithProfilingCallback;
import org.killbill.commons.profiling.ProfilingFeature.ProfilingFeatureType;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

public class ContextClassLoaderHelper {


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

    public static <T> T getWrappedServiceWithCorrectContextClassLoader(final T service, @Nullable final MetricRegistry metricRegistry, @Nullable final Map<String, Histogram> perPluginCallMetrics) {

        final Class<T> serviceClass = (Class<T>) service.getClass();
        final List<Class> allServiceInterfaces = getAllInterfaces(serviceClass);
        final Class[] serviceClassInterfaces = allServiceInterfaces.toArray(new Class[allServiceInterfaces.size()]);

        final InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

                final ClassLoader initialContextClassLoader = Thread.currentThread().getContextClassLoader();

                final String pluginServiceMethodKey = service.getClass().getSimpleName() + ":" + method.getName();
                final Histogram histogram = getOrCreateHistogram(perPluginCallMetrics, metricRegistry, pluginServiceMethodKey);
                final long beforeCall = System.nanoTime();
                try {
                    Thread.currentThread().setContextClassLoader(serviceClass.getClassLoader());
                    final Profiling<Object> prof = new Profiling<Object>();
                    return prof.executeWithProfiling(ProfilingFeatureType.PLUGIN, pluginServiceMethodKey, new WithProfilingCallback() {
                        @Override
                        public Object execute() throws Throwable {
                            return method.invoke(service, args);
                        }
                    });
                } catch (final InvocationTargetException e) {
                    if (e.getCause() != null) {
                        throw e.getCause();
                    } else {
                        throw new RuntimeException(e);
                    }
                } finally {
                    histogram.update((System.nanoTime() - beforeCall) / 1000000);
                    Thread.currentThread().setContextClassLoader(initialContextClassLoader);
                }
            }
        };
        final T wrappedService = (T) Proxy.newProxyInstance(serviceClass.getClassLoader(),
                                                            serviceClassInterfaces,
                                                            handler);
        return wrappedService;
    }

    private static Histogram getOrCreateHistogram( final Map<String, Histogram> perPluginCallMetrics, final MetricRegistry metricRegistry, final String key) {
        Histogram result =  perPluginCallMetrics.get(key);
        if (result == null) {
            synchronized (perPluginCallMetrics) {
                result =  perPluginCallMetrics.get(key);
                if (result == null) {
                    result =  metricRegistry.histogram(MetricRegistry.name(key));
                    perPluginCallMetrics.put(key, result);
                }
            }
        }
        return result;
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
}
