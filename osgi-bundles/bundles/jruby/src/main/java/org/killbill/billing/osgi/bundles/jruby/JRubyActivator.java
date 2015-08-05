/*
 * Copyright 2010-2012 Ning, Inc.
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.killbill.billing.osgi.api.config.PluginConfig.PluginType;
import org.killbill.billing.osgi.api.config.PluginConfigServiceApi;
import org.killbill.billing.osgi.api.config.PluginRubyConfig;
import org.killbill.commons.concurrent.Executors;
import org.killbill.killbill.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.killbill.osgi.libs.killbill.KillbillServiceListener;
import org.killbill.killbill.osgi.libs.killbill.KillbillServiceListenerCallback;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import com.google.common.base.Objects;

/**
 * Nomenclature:
 * - 'plugin' is the java wrapper that in this jar (JRubyPlugin and sub-classes)
 * - 'pluginMain' is the auto-generated api that is charge of the ruby <-> java translation
 * - config.getRubyMainClass() (i.e: pluginMainClass in JRubyPlugin) is the delegate, that is the real ruby plugin code, which in some case (PaymentPluginApi )is also pseudo-generated)
 */
public class JRubyActivator extends KillbillActivatorBase {

    private static final String JRUBY_PLUGINS_CONF_DIR = "org.killbill.billing.osgi.bundles.jruby.conf.dir";
    private static final String JRUBY_PLUGINS_RESTART_DELAY_SECS = "org.killbill.billing.osgi.bundles.jruby.restart.delay.secs";

    private static final String TMP_DIR_NAME = "tmp";
    private static final String RESTART_FILE_NAME = "restart.txt";

    private JRubyPlugin plugin = null;
    private ScheduledFuture<?> restartFuture = null;

    private static final String KILLBILL_PLUGIN_JPAYMENT = "Killbill::Plugin::Api::PaymentPluginApi";
    private static final String KILLBILL_PLUGIN_JNOTIFICATION = "Killbill::Plugin::Api::NotificationPluginApi";
    private static final String KILLBILL_PLUGIN_JINVOICE = "Killbill::Plugin::Api::InvoicePluginApi";
    private static final String KILLBILL_PLUGIN_JCURRENCY = "Killbill::Plugin::Api::CurrencyPluginApi";
    private static final String KILLBILL_PLUGIN_JPAYMENT_CONTROL = "Killbill::Plugin::Api::PaymentControlPluginApi";
    private static final String KILLBILL_PLUGIN_JCATALOG = "Killbill::Plugin::Api::CatalogPluginApi";

    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final String osgiKillbillClass = "org.killbill.billing.osgi.api.OSGIKillbill";
        final KillbillServiceListenerCallback listenerCallback = new KillbillServiceListenerCallback() {
            @Override
            public void isRegistered(final BundleContext context) {
                startWithContextClassLoader(context);
            }
        };
        KillbillServiceListener.listenForService(context, osgiKillbillClass, listenerCallback);
    }

    private void startWithContextClassLoader(final BundleContext context) {
        if (restartFuture != null) {
            // If the restart future is already configured, the plugin had already started
            return;
        }

        withContextClassLoader(new PluginCall() {
            @Override
            public void doCall() {
                logService.log(LogService.LOG_INFO, "JRuby bundle activated");

                // Retrieve the plugin config
                final PluginRubyConfig rubyConfig = retrievePluginRubyConfig(context);

                // Setup JRuby
                final String pluginMain;
                if (PluginType.NOTIFICATION.equals(rubyConfig.getPluginType())) {
                    plugin = new JRubyNotificationPlugin(rubyConfig, context, logService, configProperties);
                    pluginMain = KILLBILL_PLUGIN_JNOTIFICATION;
                } else if (PluginType.PAYMENT.equals(rubyConfig.getPluginType())) {
                    plugin = new JRubyPaymentPlugin(rubyConfig, context, logService, configProperties);
                    pluginMain = KILLBILL_PLUGIN_JPAYMENT;
                } else if (PluginType.INVOICE.equals(rubyConfig.getPluginType())) {
                    plugin = new JRubyInvoicePlugin(rubyConfig, context, logService, configProperties);
                    pluginMain = KILLBILL_PLUGIN_JINVOICE;
                } else if (PluginType.CURRENCY.equals(rubyConfig.getPluginType())) {
                    plugin = new JRubyCurrencyPlugin(rubyConfig, context, logService, configProperties);
                    pluginMain = KILLBILL_PLUGIN_JCURRENCY;
                } else if (PluginType.PAYMENT_CONTROL.equals(rubyConfig.getPluginType())) {
                    plugin = new JRubyPaymentControlPlugin(rubyConfig, context, logService, configProperties);
                    pluginMain = KILLBILL_PLUGIN_JPAYMENT_CONTROL;
                } else if (PluginType.CATALOG.equals(rubyConfig.getPluginType())) {
                    plugin = new JRubyCatalogPlugin(rubyConfig, context, logService, configProperties);
                    pluginMain = KILLBILL_PLUGIN_JCATALOG;
                } else {
                    throw new IllegalStateException("Unsupported plugin type " + rubyConfig.getPluginType());
                }

                // Validate and instantiate the plugin
                startPlugin(rubyConfig, pluginMain, context);

                // All plugin types can now receive event notifications (register the handler only after the plugin has started)
                dispatcher.registerEventHandler((OSGIKillbillEventHandler) plugin);
            }
        }, this.getClass().getClassLoader());
    }

    private void startPlugin(final PluginRubyConfig rubyConfig, final String pluginMain, final BundleContext context) {
        final Map<String, Object> killbillServices = retrieveKillbillApis(context);
        killbillServices.put("root", rubyConfig.getPluginVersionRoot().getAbsolutePath());
        killbillServices.put("logger", logService);
        // Default to the plugin root dir if no jruby plugins specific configuration directory was specified
        killbillServices.put("conf_dir", Objects.firstNonNull(configProperties.getString(JRUBY_PLUGINS_CONF_DIR), rubyConfig.getPluginVersionRoot().getAbsolutePath()));

        // Start the plugin synchronously
        doStartPlugin(pluginMain, context, killbillServices);

        // Setup the restart mechanism. This is useful for hotswapping plugin code
        // The principle is similar to the one in Phusion Passenger:
        // http://www.modrails.com/documentation/Users%20guide%20Apache.html#_redeploying_restarting_the_ruby_on_rails_application
        final File tmpDirPath = new File(rubyConfig.getPluginVersionRoot().getAbsolutePath() + "/" + TMP_DIR_NAME);
        if (!tmpDirPath.exists()) {
            if (!tmpDirPath.mkdir()) {
                logService.log(LogService.LOG_WARNING, "Unable to create directory " + tmpDirPath + ", the restart mechanism is disabled");
                return;
            }
        }
        if (!tmpDirPath.isDirectory()) {
            logService.log(LogService.LOG_WARNING, tmpDirPath + " is not a directory, the restart mechanism is disabled");
            return;
        }

        final Integer restart_delay_sec = Integer.parseInt((Objects.firstNonNull(configProperties.getString(JRUBY_PLUGINS_RESTART_DELAY_SECS), "5")));
        restartFuture = Executors.newSingleThreadScheduledExecutor("jruby-restarter-" + pluginMain)
                                 .scheduleWithFixedDelay(new Runnable() {
                                     long lastRestartMillis = System.currentTimeMillis();

                                     @Override
                                     public void run() {

                                         final File restartFile = new File(tmpDirPath + "/" + RESTART_FILE_NAME);
                                         if (!restartFile.isFile()) {
                                             return;
                                         }

                                         if (restartFile.lastModified() > lastRestartMillis) {
                                             logService.log(LogService.LOG_INFO, "Restarting JRuby plugin " + rubyConfig.getRubyMainClass());

                                             doStopPlugin(context);
                                             doStartPlugin(pluginMain, context, killbillServices);

                                             lastRestartMillis = restartFile.lastModified();
                                         }
                                     }
                                 }, restart_delay_sec, restart_delay_sec, TimeUnit.SECONDS);
    }

    private PluginRubyConfig retrievePluginRubyConfig(final BundleContext context) {
        final PluginConfigServiceApi pluginConfigServiceApi = killbillAPI.getPluginConfigServiceApi();
        return pluginConfigServiceApi.getPluginRubyConfig(context.getBundle().getBundleId());
    }

    public void stop(final BundleContext context) throws Exception {

        withContextClassLoader(new PluginCall() {
            @Override
            public void doCall() {
                if (restartFuture != null) {
                    restartFuture.cancel(true);
                }
                doStopPlugin(context);
                killbillAPI.close();
                logService.close();
            }
        }, this.getClass().getClassLoader());
    }

    private void doStartPlugin(final String pluginMain, final BundleContext context, final Map<String, Object> killbillServices) {
        logService.log(LogService.LOG_INFO, "Starting JRuby plugin " + pluginMain);
        plugin.instantiatePlugin(killbillServices, pluginMain);
        plugin.startPlugin(context);
        logService.log(LogService.LOG_INFO, "JRuby plugin " + pluginMain + " started");
    }

    private void doStopPlugin(final BundleContext context) {
        logService.log(LogService.LOG_INFO, "Stopping JRuby plugin " + context.getBundle().getSymbolicName());
        plugin.stopPlugin(context);
        plugin.unInstantiatePlugin();
        logService.log(LogService.LOG_INFO, "Stopped JRuby plugin " + context.getBundle().getSymbolicName());
    }

    // We make the explicit registration in the start method by hand as this would be called too early
    // (see OSGIKillbillEventDispatcher)
    @Override
    public OSGIKillbillEventHandler getOSGIKillbillEventHandler() {
        return null;
    }

    private Map<String, Object> retrieveKillbillApis(final BundleContext context) {
        final Map<String, Object> killbillUserApis = new HashMap<String, Object>();

        // See killbill/killbill_api.rb for the naming convention magic
        killbillUserApis.put("account_user_api", killbillAPI.getAccountUserApi());
        killbillUserApis.put("catalog_user_api", killbillAPI.getCatalogUserApi());
        killbillUserApis.put("subscription_api", killbillAPI.getSubscriptionApi());
        killbillUserApis.put("invoice_user_api", killbillAPI.getInvoiceUserApi());
        killbillUserApis.put("invoice_payment_api", killbillAPI.getInvoicePaymentApi());
        killbillUserApis.put("payment_api", killbillAPI.getPaymentApi());
        killbillUserApis.put("tenant_user_api", killbillAPI.getTenantUserApi());
        killbillUserApis.put("usage_user_api", killbillAPI.getUsageUserApi());
        killbillUserApis.put("custom_field_user_api", killbillAPI.getCustomFieldUserApi());
        killbillUserApis.put("tag_user_api", killbillAPI.getTagUserApi());
        killbillUserApis.put("entitlement_api", killbillAPI.getEntitlementApi());
        killbillUserApis.put("currency_conversion_api", killbillAPI.getCurrencyConversionApi());
        return killbillUserApis;
    }

    private static interface PluginCall {

        public void doCall();
    }

    // JRuby/Felix specifics, it works out of the box on Equinox.
    // Other OSGI frameworks are untested.
    private void withContextClassLoader(final PluginCall call, final ClassLoader pluginClassLoader) {
        final ClassLoader enteringContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(pluginClassLoader);
            call.doCall();
        } finally {
            // We want to make sure that calling thread gets back its original callcontext class loader when it returns
            Thread.currentThread().setContextClassLoader(enteringContextClassLoader);
        }
    }
}
