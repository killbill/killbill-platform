/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
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

package org.killbill.killbill.osgi.libs.killbill;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.killbill.billing.osgi.api.OSGIKillbillRegistrar;
import org.killbill.billing.osgi.api.config.PluginConfig;
import org.killbill.billing.osgi.api.config.PluginConfigServiceApi;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIFrameworkEventHandler;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public abstract class KillbillActivatorBase implements BundleActivator {

    @Deprecated
    private static final String JRUBY_PLUGINS_RESTART_DELAY_SECS = "org.killbill.billing.osgi.bundles.jruby.restart.delay.secs";
    private static final String PLUGINS_RESTART_DELAY_SECS = "org.killbill.billing.osgi.bundles.restart.delay.secs";

    public static final String TMP_DIR_NAME = "tmp";
    public static final String RESTART_FILE_NAME = "restart.txt";
    public static final String STOP_FILE_NAME = "stop.txt";

    protected OSGIKillbillAPI killbillAPI;
    protected OSGIKillbillLogService logService;
    protected OSGIKillbillRegistrar registrar;
    protected OSGIKillbillDataSource dataSource;
    protected OSGIKillbillEventDispatcher dispatcher;
    protected OSGIConfigPropertiesService configProperties;

    protected File tmpDir = null;

    private ScheduledFuture<?> restartFuture = null;

    @Override
    public void start(final BundleContext context) throws Exception {
        // Tracked resource
        killbillAPI = new OSGIKillbillAPI(context);
        logService = new OSGIKillbillLogService(context);
        configureSLF4JBinding();
        dataSource = new OSGIKillbillDataSource(context);
        dispatcher = new OSGIKillbillEventDispatcher(context);
        configProperties = new OSGIConfigPropertiesService(context);

        // Registrar for bundle
        registrar = new OSGIKillbillRegistrar();

        // Killbill events
        final OSGIKillbillEventHandler handler = getOSGIKillbillEventHandler();
        if (handler != null) {
            dispatcher.registerEventHandler(handler);
        }

        // OSGI Framework events
        final OSGIFrameworkEventHandler frameworkEventHandler = getOSGIFrameworkEventHandler();
        if (frameworkEventHandler != null) {
            dispatcher.registerEventHandler(frameworkEventHandler);
        }

        final PluginConfig pluginConfig = retrievePluginConfig(context);
        tmpDir = setupTmpDir(pluginConfig);

        setupRestartMechanism(pluginConfig, context);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (restartFuture != null) {
            restartFuture.cancel(true);
        }

        stopAllButRestartMechanism();
    }

    protected void stopAllButRestartMechanism() throws Exception {
        // Close trackers
        if (killbillAPI != null) {
            killbillAPI.close();
            killbillAPI = null;
        }
        if (dispatcher != null) {
            dispatcher.close();
            dispatcher = null;
        }
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
        if (logService != null) {
            logService.close();
            logService = null;
        }

        try {
            // Remove Killbill event handler
            final OSGIKillbillEventHandler handler = getOSGIKillbillEventHandler();
            if (handler != null && dispatcher != null) {
                dispatcher.unregisterEventHandler(handler);
                dispatcher = null;
            }
        } catch (final OSGIServiceNotAvailable ignore) {
            // If the system bundle shut down prior to that bundle, we can' unregister our Observer, which is fine.
        }

        // Unregister all services from that bundle
        if (registrar != null) {
            registrar.unregisterAll();
            registrar = null;
        }
    }

    public abstract OSGIKillbillEventHandler getOSGIKillbillEventHandler();

    public OSGIFrameworkEventHandler getOSGIFrameworkEventHandler() { return null; }

    protected void configureSLF4JBinding() {
        try {
            // KillbillActivatorBase.class.getClassLoader() is the WebAppClassLoader (org.killbill.killbill.osgi.libs.killbill is exported)
            // Make sure to use the BundleClassLoader instead
            final Class<?> staticLoggerBinderClass = this.getClass()
                                                         .getClassLoader()
                                                         .loadClass("org.slf4j.impl.StaticLoggerBinder");

            final Object staticLoggerBinder = staticLoggerBinderClass.getMethod("getSingleton")
                                                                     .invoke(null);

            staticLoggerBinderClass.getMethod("setLogService", LogService.class)
                                   .invoke(staticLoggerBinder, logService);
        } catch (final ClassNotFoundException e) {
            logService.log(LogService.LOG_WARNING, "Unable to redirect SLF4J logs", e);
        } catch (final InvocationTargetException e) {
            logService.log(LogService.LOG_WARNING, "Unable to redirect SLF4J logs", e);
        } catch (final NoSuchMethodException e) {
            // Don't log the full stack trace for backward compatibility (old plugins would throw NoSuchMethodException)
            logService.log(LogService.LOG_WARNING, "Unable to redirect SLF4J logs");
        } catch (final IllegalAccessException e) {
            logService.log(LogService.LOG_WARNING, "Unable to redirect SLF4J logs", e);
        }
    }

    protected PluginConfig retrievePluginConfig(final BundleContext context) {
        final PluginConfigServiceApi pluginConfigServiceApi = killbillAPI.getPluginConfigServiceApi();
        return pluginConfigServiceApi.getPluginJavaConfig(context.getBundle().getBundleId());
    }

    // Setup the restart mechanism. This is useful for hotswapping plugin code
    // The principle is similar to the one in Phusion Passenger:
    // http://www.modrails.com/documentation/Users%20guide%20Apache.html#_redeploying_restarting_the_ruby_on_rails_application
    private void setupRestartMechanism(final PluginConfig pluginConfig, final BundleContext context) {
        if (tmpDir == null || restartFuture != null) {
            return;
        }

        String restartDelaySecProperty = configProperties.getString(JRUBY_PLUGINS_RESTART_DELAY_SECS);
        if (restartDelaySecProperty == null) {
            restartDelaySecProperty = configProperties.getString(PLUGINS_RESTART_DELAY_SECS);
        }
        final Integer restartDelaySecs = restartDelaySecProperty == null ? 5 : Integer.parseInt(restartDelaySecProperty);
        restartFuture = Executors.newSingleThreadScheduledExecutor()
                                 .scheduleWithFixedDelay(new Runnable() {
                                                             long lastRestartMillis = System.currentTimeMillis();

                                                             @Override
                                                             public void run() {
                                                                 final boolean shouldStopPlugin = shouldStopPlugin();
                                                                 if (shouldStopPlugin) {
                                                                     try {
                                                                         logSafely(LogService.LOG_INFO, "Stopping plugin " + pluginConfig.getPluginName());
                                                                         stopAllButRestartMechanism();
                                                                     } catch (final IllegalStateException e) {
                                                                         // Ignore errors from JRubyPlugin.checkPluginIsRunning, which can happen during development
                                                                         logSafely(LogService.LOG_DEBUG, "Error stopping plugin " + pluginConfig.getPluginName(), e);
                                                                     } catch (final Exception e) {
                                                                         logSafely(LogService.LOG_WARNING, "Error stopping plugin " + pluginConfig.getPluginName(), e);
                                                                     }
                                                                     return;
                                                                 }

                                                                 final Long lastRestartTime = lastRestartTime();
                                                                 if (lastRestartTime != null && lastRestartTime > lastRestartMillis) {
                                                                     logSafely(LogService.LOG_INFO, "Restarting plugin " + pluginConfig.getPluginName());

                                                                     try {
                                                                         stopAllButRestartMechanism();
                                                                     } catch (final IllegalStateException e) {
                                                                         // Ignore errors from JRubyPlugin.checkPluginIsRunning, which can happen during development
                                                                         logSafely(LogService.LOG_DEBUG, "Error stopping plugin " + pluginConfig.getPluginName(), e);
                                                                     } catch (final Exception e) {
                                                                         logSafely(LogService.LOG_WARNING, "Error stopping plugin " + pluginConfig.getPluginName(), e);
                                                                     }

                                                                     try {
                                                                         start(context);
                                                                     } catch (final Exception e) {
                                                                         logSafely(LogService.LOG_WARNING, "Error starting plugin " + pluginConfig.getPluginName(), e);
                                                                     }

                                                                     lastRestartMillis = lastRestartTime;
                                                                 }
                                                             }
                                                         },
                                                         restartDelaySecs,
                                                         restartDelaySecs,
                                                         TimeUnit.SECONDS);
    }

    protected boolean shouldStopPlugin() {
        final File stopFile = new File(tmpDir + "/" + STOP_FILE_NAME);
        return stopFile.isFile();
    }

    protected Long lastRestartTime() {
        final File restartFile = new File(tmpDir + "/" + RESTART_FILE_NAME);
        if (!restartFile.isFile()) {
            return null;
        } else {
            return restartFile.lastModified();
        }
    }

    private void logSafely(final int level, final String message) {
        if (logService != null) {
            logService.log(level, message);
        }
    }

    private void logSafely(final int level, final String message, final Throwable exception) {
        if (logService != null) {
            logService.log(level, message, exception);
        }
    }

    private File setupTmpDir(final PluginConfig pluginConfig) {
        final File tmpDirPath = new File(pluginConfig.getPluginVersionRoot().getAbsolutePath() + "/" + TMP_DIR_NAME);
        if (!tmpDirPath.exists()) {
            if (!tmpDirPath.mkdir()) {
                logService.log(LogService.LOG_WARNING, "Unable to create directory " + tmpDirPath + ", the restart mechanism is disabled");
                return null;
            }
        }
        if (!tmpDirPath.isDirectory()) {
            logService.log(LogService.LOG_WARNING, tmpDirPath + " is not a directory, the restart mechanism is disabled");
            return null;
        }
        return tmpDirPath;
    }
}
