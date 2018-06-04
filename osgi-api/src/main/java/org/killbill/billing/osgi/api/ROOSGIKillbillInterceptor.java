/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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

package org.killbill.billing.osgi.api;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;

public class ROOSGIKillbillInterceptor<T> implements InvocationHandler {

    private final T t;

    public ROOSGIKillbillInterceptor(final T t) {
        this.t = t;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getProxy(final T t, final Class<? super T> interfaceType) {
        final InvocationHandler handler = new ROOSGIKillbillInterceptor(t);
        return (T) Proxy.newProxyInstance(interfaceType.getClassLoader(),
                                          new Class<?>[]{interfaceType},
                                          handler);
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final Object[] newArgs = args.length > 0 ? new Object[args.length] : args;
        for (int i = 0; i < args.length; i++) {
            final Object argument = args[i];
            if (argument instanceof TenantContext && !(argument instanceof CallContext)) {
                newArgs[i] = new ROTenantContext((TenantContext) argument);
            } else {
                newArgs[i] = argument;
            }
        }

        try {
            return method.invoke(t, newArgs);
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
