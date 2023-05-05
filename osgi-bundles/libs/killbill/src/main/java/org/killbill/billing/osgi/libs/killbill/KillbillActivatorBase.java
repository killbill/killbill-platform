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

package org.killbill.billing.osgi.libs.killbill;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.killbill.billing.osgi.api.OSGIKillbillRegistrar;
import org.killbill.billing.osgi.api.config.PluginConfig;
import org.killbill.billing.osgi.api.config.PluginConfigServiceApi;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class KillbillActivatorBase implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(KillbillActivatorBase.class);

    private ScheduledExecutorService restartMechanismExecutorService = null;

    private static final String PLUGINS_RESTART_DELAY_SECS = "org.killbill.billing.osgi.bundles.restart.delay.secs";

    public static final String TMP_DIR_NAME = "tmp";
    public static final String RESTART_FILE_NAME = "restart.txt";
    public static final String DISABLED_FILE_NAME = "disabled.txt";

    protected OSGIKillbillAPI killbillAPI;
    protected ROOSGIKillbillAPI roOSGIkillbillAPI;
    protected OSGIKillbillLogService logService;
    protected OSGIKillbillRegistrar registrar;
    protected OSGIKillbillDataSource dataSource;
    protected OSGIKillbillClock clock;
    protected OSGIKillbillEventDispatcher dispatcher;
    protected OSGIConfigPropertiesService configProperties;
    protected OSGIMetricRegistry metricRegistry;

    protected File tmpDir = null;

    private ScheduledFuture<?> restartFuture = null;

    @Override
    public void start(final BundleContext context) throws Exception {
        // Keep it for now for backward compatibility purposes with existing plugins
        logService = new OSGIKillbillLogService(context);

        logger.info("OSGI bundle='{}' received START command", context.getBundle().getSymbolicName());

        killbillAPI = new OSGIKillbillAPI(context);
        roOSGIkillbillAPI = new ROOSGIKillbillAPI(context);
        dataSource = new OSGIKillbillDataSource(context);
        dispatcher = new OSGIKillbillEventDispatcher(context);
        configProperties = new OSGIConfigPropertiesService(context);
        clock = new OSGIKillbillClock(context);
        metricRegistry = new OSGIMetricRegistry(context);

        // Registrar for bundle
        registrar = new OSGIKillbillRegistrar();

        final PluginConfig pluginConfig = retrievePluginConfig(context);
        // pluginConfig is null for pure OSGI bundles
        if (pluginConfig != null) {
            tmpDir = setupTmpDir(pluginConfig);
            setupRestartMechanism(pluginConfig, context);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        logger.info("OSGI bundle='{}' received STOP command", context.getBundle().getSymbolicName());

        if (restartFuture != null) {
            restartFuture.cancel(true);
        }

        if (restartMechanismExecutorService != null) {
            restartMechanismExecutorService.shutdownNow();
        }

        stopAllButRestartMechanism(context);
    }

    protected void stopAllButRestartMechanism(final BundleContext context) throws Exception {
        // Remove provide services to other bundles first
        if (registrar != null) {
            registrar.unregisterAll();
            registrar = null;
        }
        // Then, un-register all handlers
        try {
            if (dispatcher != null) {
                dispatcher.unregisterAllHandlers();
            }
        } catch (final OSGIServiceNotAvailable ignore) {
            logger.warn("OSGI bundle='{}' failed to unregister killbill handler", context.getBundle().getSymbolicName());
        } finally {
            if (dispatcher != null) {
                dispatcher.close();
                dispatcher = null;
            }
        }
        // Finally close all trackers, ending by logging to make sure bundle can log as far as possible.
        if (killbillAPI != null) {
            killbillAPI.close();
            killbillAPI = null;
        }
        if (roOSGIkillbillAPI != null) {
            roOSGIkillbillAPI.close();
            roOSGIkillbillAPI = null;
        }
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
        if (logService != null) {
            logService.close();
            logService = null;
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

        final String restartDelaySecProperty = configProperties.getString(PLUGINS_RESTART_DELAY_SECS);
        final Integer restartDelaySecs = restartDelaySecProperty == null ? 5 : Integer.parseInt(restartDelaySecProperty);

        restartMechanismExecutorService = Executors.newSingleThreadScheduledExecutor();
        restartFuture = restartMechanismExecutorService
                                 .scheduleWithFixedDelay(new Runnable() {
                                                             long lastRestartMillis = System.currentTimeMillis();

                                                             @Override
                                                             public void run() {
                                                                 final boolean shouldStopPlugin = shouldStopPlugin();
                                                                 if (shouldStopPlugin) {
                                                                     try {
                                                                         logger.info("Stopping plugin='{}' ", pluginConfig.getPluginName());
                                                                         stopAllButRestartMechanism(context);
                                                                     } catch (final Exception e) {
                                                                         logger.warn("Error stopping plugin='{}'", pluginConfig.getPluginName(), e);
                                                                     }
                                                                     return;
                                                                 }

                                                                 final Long lastRestartTime = lastRestartTime();
                                                                 if (lastRestartTime != null && lastRestartTime > lastRestartMillis) {
                                                                     logger.info("Restarting plugin='{}'", pluginConfig.getPluginName());

                                                                     try {
                                                                         stopAllButRestartMechanism(context);
                                                                     } catch (final Exception e) {
                                                                         logger.warn("Error stopping plugin='{}'", pluginConfig.getPluginName(), e);
                                                                     }

                                                                     try {
                                                                         start(context);
                                                                     } catch (final Exception e) {
                                                                         logger.warn("Error starting plugin='{}'", pluginConfig.getPluginName(), e);
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
        final File stopFile = new File(tmpDir + "/" + DISABLED_FILE_NAME);
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

    private File setupTmpDir(final PluginConfig pluginConfig) {
        final File tmpDirPath = new File(pluginConfig.getPluginVersionRoot().getAbsolutePath() + "/" + TMP_DIR_NAME);
        if (!tmpDirPath.exists()) {
            if (!tmpDirPath.mkdir()) {
                logger.warn("Unable to create directory {}, the restart mechanism is disabled", tmpDirPath);
                return null;
            }
        }
        if (!tmpDirPath.isDirectory()) {
            logger.warn("{} is not a directory, the restart mechanism is disabled", tmpDirPath);
            return null;
        }
        return tmpDirPath;
    }
}
