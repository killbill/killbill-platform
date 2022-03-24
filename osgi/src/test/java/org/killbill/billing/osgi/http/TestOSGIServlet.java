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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.killbill.billing.osgi.ContextClassLoaderHelper;
import org.killbill.commons.metrics.api.MetricRegistry;
import org.killbill.commons.metrics.impl.NoOpMetricRegistry;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestOSGIServlet {

    @Test(groups = "fast")
    public void testNoDuplicateServlets() throws Exception {
        testNoDuplicateServlets(false);
    }

    @Test(groups = "fast")
    public void testNoDuplicateServletsWithWrapping() throws Exception {
        testNoDuplicateServlets(true);
    }

    public void testNoDuplicateServlets(final boolean withWrapping) throws Exception {
        final AtomicLong paymentRetriesPluginInvocationCount = new AtomicLong(0);
        final Servlet paymentRetriesPluginServlet = new HttpServlet() {
            @Override
            public void service(final ServletRequest req, final ServletResponse res) throws ServletException, IOException {
                paymentRetriesPluginInvocationCount.incrementAndGet();
            }
        };
        Assert.assertEquals(paymentRetriesPluginInvocationCount.get(), 0);

        final AtomicLong anotherPluginInvocationCount = new AtomicLong(0);
        final Servlet anotherPluginServlet = new HttpServlet() {
            @Override
            public void service(final ServletRequest req, final ServletResponse res) throws ServletException, IOException {
                anotherPluginInvocationCount.incrementAndGet();
            }
        };
        Assert.assertEquals(anotherPluginInvocationCount.get(), 0);

        final OSGIServlet osgiServlet = new OSGIServlet();
        osgiServlet.servletRouter = new DefaultServletRouter();
        if (withWrapping) {
            final MetricRegistry metricRegistry = new NoOpMetricRegistry();
            osgiServlet.servletRouter.registerServiceFromPath("/payment-retries-plugin", ContextClassLoaderHelper.getWrappedServiceWithCorrectContextClassLoader(paymentRetriesPluginServlet, Servlet.class, "/payment-retries-plugin", metricRegistry));
            osgiServlet.servletRouter.registerServiceFromPath("/another-plugin", ContextClassLoaderHelper.getWrappedServiceWithCorrectContextClassLoader(anotherPluginServlet, Servlet.class, "/another-plugin", metricRegistry));
        } else {
            osgiServlet.servletRouter.registerServiceFromPath("/payment-retries-plugin", paymentRetriesPluginServlet);
            osgiServlet.servletRouter.registerServiceFromPath("/another-plugin", anotherPluginServlet);
        }
        Assert.assertEquals(osgiServlet.initializedServlets.size(), 0);

        final HttpServletRequest paymentRetriesReq = Mockito.mock(HttpServletRequest.class);
        Mockito.when(paymentRetriesReq.getAttribute("killbill.osgi.servletConfig")).thenReturn(Mockito.mock(ServletConfig.class));
        Mockito.when(paymentRetriesReq.getServletPath()).thenReturn("");
        Mockito.when(paymentRetriesReq.getPathInfo()).thenReturn("/payment-retries-plugin/configuration");

        final HttpServletRequest anotherPluginReq = Mockito.mock(HttpServletRequest.class);
        Mockito.when(anotherPluginReq.getAttribute("killbill.osgi.servletConfig")).thenReturn(Mockito.mock(ServletConfig.class));
        Mockito.when(anotherPluginReq.getServletPath()).thenReturn("");
        Mockito.when(anotherPluginReq.getPathInfo()).thenReturn("/another-plugin");

        final HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);

        osgiServlet.doGet(paymentRetriesReq, resp);
        Assert.assertEquals(paymentRetriesPluginInvocationCount.get(), 1);
        Assert.assertEquals(anotherPluginInvocationCount.get(), 0);
        Assert.assertEquals(osgiServlet.initializedServlets.size(), 1);

        osgiServlet.doGet(paymentRetriesReq, resp);
        Assert.assertEquals(paymentRetriesPluginInvocationCount.get(), 2);
        Assert.assertEquals(anotherPluginInvocationCount.get(), 0);
        Assert.assertEquals(osgiServlet.initializedServlets.size(), 1);

        osgiServlet.doGet(anotherPluginReq, resp);
        Assert.assertEquals(paymentRetriesPluginInvocationCount.get(), 2);
        Assert.assertEquals(anotherPluginInvocationCount.get(), 1);
        Assert.assertEquals(osgiServlet.initializedServlets.size(), 2);

        osgiServlet.doGet(anotherPluginReq, resp);
        Assert.assertEquals(paymentRetriesPluginInvocationCount.get(), 2);
        Assert.assertEquals(anotherPluginInvocationCount.get(), 2);
        Assert.assertEquals(osgiServlet.initializedServlets.size(), 2);
    }
}
