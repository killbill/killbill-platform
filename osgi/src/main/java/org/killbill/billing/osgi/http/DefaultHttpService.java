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

package org.killbill.billing.osgi.http;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.killbill.billing.osgi.ContextClassLoaderHelper;
import org.killbill.commons.metrics.api.MetricRegistry;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

@Singleton
public class DefaultHttpService implements HttpService {

    private final DefaultServletRouter servletRouter;
    private final MetricRegistry metricsRegistry;

    @Inject
    public DefaultHttpService(final DefaultServletRouter servletRouter, final MetricRegistry metricsRegistry) {
        this.servletRouter = servletRouter;
        this.metricsRegistry = metricsRegistry;
    }

    @Override
    public void registerServlet(final String alias, final Servlet servlet, final Dictionary initparams, final HttpContext httpContext) throws ServletException {

        if (alias == null) {
            throw new IllegalArgumentException("Invalid alias (null)");
        } else if (servlet == null) {
            throw new IllegalArgumentException("Invalid servlet (null)");
        }
        final Servlet wrappedServlet = ContextClassLoaderHelper.getWrappedServiceWithCorrectContextClassLoader(servlet, Servlet.class, alias, metricsRegistry);

        servletRouter.registerServiceFromPath(alias, wrappedServlet);
    }

    @Override
    public void registerResources(final String alias, final String name, final HttpContext httpContext) throws NamespaceException {
        final Servlet staticServlet = new StaticServlet(httpContext);
        try {
            registerServlet(alias, staticServlet, new Hashtable(), httpContext);
        } catch (final ServletException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void unregister(final String alias) {
        servletRouter.unregisterServiceFromPath(alias);
    }

    @Override
    public HttpContext createDefaultHttpContext() {
        return new DefaultHttpContext();
    }
}
