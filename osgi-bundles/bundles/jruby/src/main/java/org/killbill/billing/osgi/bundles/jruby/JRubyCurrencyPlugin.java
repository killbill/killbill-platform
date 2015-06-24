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

package org.killbill.billing.osgi.bundles.jruby;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.SortedSet;

import org.joda.time.DateTime;
import org.jruby.Ruby;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.currency.api.Rate;
import org.killbill.billing.currency.plugin.api.CurrencyPluginApi;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.api.config.PluginRubyConfig;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

public class JRubyCurrencyPlugin extends JRubyNotificationPlugin implements CurrencyPluginApi {

    private volatile ServiceRegistration currencyPluginRegistration;

    public JRubyCurrencyPlugin(final PluginRubyConfig config, final BundleContext bundleContext, final LogService logger, final OSGIConfigPropertiesService configProperties) {
        super(config, bundleContext, logger, configProperties);
    }

    @Override
    public void startPlugin(final BundleContext context) {
        super.startPlugin(context);

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("name", pluginMainClass);
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, pluginGemName);
        currencyPluginRegistration = context.registerService(CurrencyPluginApi.class.getName(), this, props);
    }

    @Override
    public void stopPlugin(final BundleContext context) {
        if (currencyPluginRegistration != null) {
            currencyPluginRegistration.unregister();
        }
        super.stopPlugin(context);
    }

    @Override
    public Set<Currency> getBaseCurrencies() {
        return callWithRuntimeAndChecking(new PluginCallback<Set<Currency>, RuntimeException>() {
            @Override
            public Set<Currency> doCall(final Ruby runtime) {
                return ((CurrencyPluginApi) pluginInstance).getBaseCurrencies();
            }
        });
    }

    @Override
    public DateTime getLatestConversionDate(final Currency currency) {
        return callWithRuntimeAndChecking(new PluginCallback<DateTime, RuntimeException>() {
            @Override
            public DateTime doCall(final Ruby runtime) {
                return ((CurrencyPluginApi) pluginInstance).getLatestConversionDate(currency);
            }
        });
    }

    @Override
    public SortedSet<DateTime> getConversionDates(final Currency currency) {
        return callWithRuntimeAndChecking(new PluginCallback<SortedSet<DateTime>, RuntimeException>() {
            @Override
            public SortedSet<DateTime> doCall(final Ruby runtime) {
                return ((CurrencyPluginApi) pluginInstance).getConversionDates(currency);
            }
        });
    }

    @Override
    public Set<Rate> getCurrentRates(final Currency currency) {
        return callWithRuntimeAndChecking(new PluginCallback<Set<Rate>, RuntimeException>() {
            @Override
            public Set<Rate> doCall(final Ruby runtime) {
                return ((CurrencyPluginApi) pluginInstance).getCurrentRates(currency);
            }
        });
    }

    @Override
    public Set<Rate> getRates(final Currency currency, final DateTime time) {
        return callWithRuntimeAndChecking(new PluginCallback<Set<Rate>, RuntimeException>() {
            @Override
            public Set<Rate> doCall(final Ruby runtime) {
                return ((CurrencyPluginApi) pluginInstance).getRates(currency, time);
            }
        });
    }
}
