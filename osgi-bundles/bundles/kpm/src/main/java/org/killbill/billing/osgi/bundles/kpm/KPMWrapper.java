/*
 * Copyright 2014-2019 Groupon, Inc
 * Copyright 2014-2019 The Billing Project, LLC
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

package org.killbill.billing.osgi.bundles.kpm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.killbill.billing.plugin.util.http.SslUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncCompletionHandlerBase;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.Response;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.airlift.command.Command;
import io.airlift.command.CommandFailedException;
import io.airlift.units.Duration;

public class KPMWrapper {

    private static final Logger logger = LoggerFactory.getLogger(KPMWrapper.class);

    private static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.kpm.";

    private static final ExecutorService executor = Executors.newCachedThreadPool(daemonThreadsNamed("kpm-%s"));
    private static final ImmutableSet<Integer> DEFAULT_SUCCESSFUL_EXIT_CODES = ImmutableSet.of(0);
    private static final File DEFAULT_DIRECTORY = new File(".").getAbsoluteFile();
    // Be generous
    private static final Duration COMMAND_TIMEOUT = new Duration(5, TimeUnit.MINUTES);
    private static final Joiner SPACE_JOINER = Joiner.on(" ");

    private final String kpmPath;
    private final String bundlesPath;
    private final String nexusUrl;
    private final String nexusRepository;
    private final AsyncHttpClient httpClient;

    public KPMWrapper(final Properties properties) throws GeneralSecurityException {
        this.kpmPath = MoreObjects.firstNonNull(properties.getProperty(PROPERTY_PREFIX + "kpmPath"), "kpm");
        this.bundlesPath = MoreObjects.firstNonNull(properties.getProperty(PROPERTY_PREFIX + "bundlesPath"), Paths.get("var", "tmp", "bundles").toString());
        this.nexusUrl = MoreObjects.firstNonNull(properties.getProperty(PROPERTY_PREFIX + "nexusUrl"), "https://oss.sonatype.org");
        this.nexusRepository = MoreObjects.firstNonNull(properties.getProperty(PROPERTY_PREFIX + "nexusRepository"), "releases");
        this.httpClient = buildAsyncHttpClient(Boolean.valueOf(MoreObjects.firstNonNull(properties.getProperty(PROPERTY_PREFIX + "strictSSL"), "true")),
                                               Integer.parseInt(MoreObjects.firstNonNull(properties.getProperty(PROPERTY_PREFIX + "readTimeoutSec"), "60")) * 1000,
                                               Integer.parseInt(MoreObjects.firstNonNull(properties.getProperty(PROPERTY_PREFIX + "connectTimeoutSec"), "60")) * 1000);
    }

    public String getAvailablePlugins(final String kbVersion, final Boolean latest) {
        final Path sha1File = Paths.get(bundlesPath, "sha1.yml");

        final List<String> commands = new LinkedList<String>();
        commands.add(kpmPath);
        commands.add("info");
        commands.add("--as-json");
        commands.add(latest ? "--force-download" : "--no-force-download");
        commands.add("--sha1-file=" + sha1File.toString());

        if (kbVersion != null) {
            commands.add("--version=" + kbVersion);
        }
        if (nexusUrl != null) {
            commands.add("--overrides=url:" + nexusUrl);
        }
        if (nexusRepository != null) {
            commands.add("--overrides=repository:" + nexusRepository);
        }

        return system(commands);
    }

    public void install(final String pluginKey, final String uri, final String pluginVersion, final String pluginType) throws IOException, ExecutionException, InterruptedException {
        logger.info("Installing pluginKey='{}', uri='{}', pluginVersion='{}', pluginType='{}'",
                    pluginKey,
                    uri,
                    pluginVersion,
                    pluginType);

        final File tmp = File.createTempFile(pluginKey, "kpm");
        try {
            final FileOutputStream stream = new FileOutputStream(tmp);
            final Response response = httpClient.prepareGet(uri)
                                                .execute(new AsyncCompletionHandlerBase() {
                                                    @Override
                                                    public STATE onBodyPartReceived(final HttpResponseBodyPart bodyPart) throws Exception {
                                                        bodyPart.writeTo(stream);
                                                        return STATE.CONTINUE;
                                                    }
                                                }).get();

            final List<String> commands = new LinkedList<String>();
            commands.add(kpmPath);
            commands.add("ruby".equals(pluginType) ? "install_ruby_plugin" : "install_java_plugin");
            commands.add(pluginKey);

            commands.add("--from-source-file=" + tmp.toString());
            if (pluginVersion != null) {
                commands.add("--version=" + pluginVersion);
            }
            if (nexusUrl != null) {
                commands.add("--overrides=url:" + nexusUrl);
            }
            if (nexusRepository != null) {
                commands.add("--overrides=repository:" + nexusRepository);
            }

            system(commands);
        } finally {
            tmp.delete();
        }
    }

    public void install(final String pluginKey,
                        final String kbVersion,
                        final String pluginArtifactId,
                        final String pluginVersion,
                        final String pluginGroupId,
                        final String pluginPackaging,
                        final String pluginClassifier,
                        final String pluginType,
                        final boolean forceDownload) {
        logger.info("Installing pluginKey='{}', kbVersion='{}', pluginArtifactId='{}', pluginVersion='{}', pluginGroupId='{}', pluginPackaging='{}', pluginClassifier='{}', pluginType='{}', forceDownload='{}'",
                    pluginKey,
                    kbVersion,
                    pluginArtifactId,
                    pluginVersion,
                    pluginGroupId,
                    pluginPackaging,
                    pluginClassifier,
                    pluginType,
                    forceDownload);

        final List<String> commands = new LinkedList<String>();
        commands.add(kpmPath);
        commands.add("ruby".equals(pluginType) ? "install_ruby_plugin" : "install_java_plugin");
        commands.add(pluginKey);
        commands.add(kbVersion);

        if (pluginArtifactId != null) {
            commands.add("--artifact_id=" + pluginArtifactId);
        }
        if (pluginGroupId != null) {
            commands.add("--group_id=" + pluginGroupId);
        }
        if (pluginVersion != null) {
            commands.add("--version=" + pluginVersion);
        }
        if (pluginPackaging != null) {
            commands.add("--packaging=" + pluginPackaging);
        }
        if (pluginClassifier != null) {
            commands.add("--classifier=" + pluginClassifier);
        }
        if (pluginVersion != null) {
            commands.add("--version=" + pluginVersion);
        }
        commands.add("--force_download=" + forceDownload);
        if (nexusUrl != null) {
            commands.add("--overrides=url:" + nexusUrl);
        }
        if (nexusRepository != null) {
            commands.add("--overrides=repository:" + nexusRepository);
        }

        system(commands);
    }

    public void uninstall(final String pluginKey, final String pluginVersion) {
        logger.info("Uninstalling plugin='{}', version='{}'", pluginKey, pluginVersion);

        final List<String> commands = new LinkedList<String>();
        commands.add(kpmPath);
        commands.add("uninstall");
        commands.add(pluginKey);

        if (pluginVersion != null) {
            commands.add("--version=" + pluginVersion);
        }

        system(commands);
    }

    private AsyncHttpClient buildAsyncHttpClient(final Boolean strictSSL, final int readTimeoutMs, final int connectTimeoutMs) throws GeneralSecurityException {
        final AsyncHttpClientConfig.Builder cfg = new AsyncHttpClientConfig.Builder();
        cfg.setUserAgent("KillBill/kpm-plugin/1.0")
           .setConnectTimeout(connectTimeoutMs)
           .setReadTimeout(readTimeoutMs);
        if (!strictSSL) {
            cfg.setSSLContext(SslUtils.getInstance().getSSLContext(!strictSSL));
        }
        return new AsyncHttpClient(cfg.build());
    }

    private String system(final List<String> commands) {
        try {
            final Command commandToExecute = new Command(commands,
                                                         DEFAULT_SUCCESSFUL_EXIT_CODES,
                                                         DEFAULT_DIRECTORY,
                                                         ImmutableMap.<String, String>of(),
                                                         COMMAND_TIMEOUT);
            logger.info("Executing: {}", SPACE_JOINER.join(commandToExecute.getCommand()));
            final String commandOutput = commandToExecute.execute(executor)
                                                         .getCommandOutput();
            logger.info("Output: {}", commandOutput);
            return commandOutput;
        } catch (final CommandFailedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a {@link ThreadFactory} that creates named daemon threads.
     * using the specified naming format.
     */
    private static ThreadFactory daemonThreadsNamed(final String nameFormat) {
        return new ThreadFactoryBuilder()
                .setNameFormat(nameFormat)
                .setDaemon(true)
                .setThreadFactory(new ContextClassLoaderThreadFactory(Thread.currentThread().getContextClassLoader()))
                .build();
    }

    private static class ContextClassLoaderThreadFactory implements ThreadFactory {

        private final ClassLoader classLoader;

        ContextClassLoaderThreadFactory(final ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public Thread newThread(final Runnable runnable) {
            final Thread thread = new Thread(runnable);
            thread.setContextClassLoader(classLoader);
            return thread;
        }
    }
}
