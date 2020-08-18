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

package org.killbill.billing.beatrix.integration.osgi.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.killbill.billing.osgi.api.config.PluginConfig;
import org.killbill.billing.osgi.api.config.PluginJavaConfig;
import org.killbill.billing.osgi.api.config.PluginLanguage;
import org.killbill.billing.osgi.api.config.PluginType;
import org.killbill.billing.osgi.config.OSGIConfig;
import org.testng.Assert;

import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;

// Install a JRuby or Java plugin programmatically
public class SetupBundleWithAssertion {

    private static final String JRUBY_BUNDLE_RESOURCE = "killbill-osgi-bundles-jruby";

    private final String bundleName;
    private final OSGIConfig config;
    private final String killbillVersion;
    private final File rootInstallDir;

    public SetupBundleWithAssertion(final String bundleName, final OSGIConfig config, final String killbillVersion) {
        this.bundleName = bundleName;
        this.config = config;
        this.killbillVersion = killbillVersion;
        this.rootInstallDir = new File(config.getRootInstallationDir());
    }

    public void setupJrubyBundle() {
        try {
            installJrubyJar();

            final URL resourceUrl = Resources.getResource(bundleName);
            final File unzippedRubyPlugin = unGzip(new File(resourceUrl.getFile()), rootInstallDir);

            final StringBuilder tmp = new StringBuilder(rootInstallDir.getAbsolutePath());
            tmp.append("/plugins/")
               .append(PluginLanguage.RUBY.toString().toLowerCase());

            final File destination = new File(tmp.toString());
            if (!destination.exists()) {
                Assert.assertTrue(destination.mkdirs(), "Unable to create directory " + destination.getAbsolutePath());
            }

            unTar(unzippedRubyPlugin, destination);
        } catch (final IOException e) {
            Assert.fail(e.getMessage());
        } catch (final ArchiveException e) {
            Assert.fail(e.getMessage());
        }
    }

    public void setupJavaBundle() {
        try {
            // Retrieve PluginConfig info from classpath
            // test bundle should have been exported under Beatrix resource by the maven maven-dependency-plugin
            final PluginJavaConfig pluginConfig = extractJavaBundleTestResource();
            Assert.assertNotNull(pluginConfig);

            // Create OSGI install bundle directory
            setupDirectoryStructure(pluginConfig);

            // Copy the jar
            ByteStreams.copy(new FileInputStream(new File(pluginConfig.getBundleJarPath())), new FileOutputStream(new File(pluginConfig.getPluginVersionRoot().getAbsolutePath(), pluginConfig.getPluginVersionnedName() + ".jar")));

            // Create the osgiConfig file
            createConfigFile(pluginConfig);

        } catch (final IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    public void cleanBundleInstallDir() {
        if (rootInstallDir.exists()) {
            deleteDirectory(rootInstallDir, false);
        }
    }

    private void createConfigFile(final PluginConfig pluginConfig) throws IOException {
        PrintStream printStream = null;
        try {
            final File configFile = new File(pluginConfig.getPluginVersionRoot(), config.getOSGIKillbillPropertyName());
            Assert.assertTrue(configFile.createNewFile(), "Unable to create file " + configFile.getAbsolutePath());
            printStream = new PrintStream(new FileOutputStream(configFile), true, StandardCharsets.UTF_8.name());
            printStream.print("pluginType=" + PluginType.NOTIFICATION);
        } finally {
            if (printStream != null) {
                printStream.close();
            }
        }
    }

    private void setupDirectoryStructure(final PluginConfig pluginConfig) {
        cleanBundleInstallDir();
        Assert.assertTrue(pluginConfig.getPluginVersionRoot().mkdirs(), "Unable to create directory " + pluginConfig.getPluginVersionRoot().getAbsolutePath());
    }

    private static void deleteDirectory(final File path, final boolean deleteParent) {
        if (path == null) {
            return;
        }

        if (path.exists()) {
            final File[] files = path.listFiles();
            if (files != null) {
                for (final File f : files) {
                    if (f.isDirectory()) {
                        deleteDirectory(f, true);
                    }
                    Assert.assertTrue(f.delete(), "Unable to delete file " + f.getAbsolutePath());
                }
            }
            if (deleteParent) {
                Assert.assertTrue(path.delete(), "Unable to delete file " + path.delete());
            }
        }
    }

    private void installJrubyJar() throws IOException {
        final String resourceName = JRUBY_BUNDLE_RESOURCE + ".jar";
        final URL resourceUrl = Resources.getResource(resourceName);
        final File rubyJarInput = new File(resourceUrl.getFile());

        final File platform = new File(rootInstallDir, "platform");
        if (!platform.exists()) {
            Assert.assertTrue(platform.mkdir(), "Unable to create directory " + platform.getAbsolutePath());
        }

        final File rubyJarDestination = new File(platform, "jruby.jar");
        try (final FileInputStream from = new FileInputStream(rubyJarInput);
             final FileOutputStream to = new FileOutputStream(rubyJarDestination)) {
            ByteStreams.copy(from, to);
        }
    }

    private PluginJavaConfig extractJavaBundleTestResource() {
        final String resourceName = bundleName + "-jar-with-dependencies.jar";
        final URL resourceUrl = Resources.getResource(resourceName);
        if (resourceUrl != null) {
            final String[] parts = resourceUrl.getPath().split("/");
            final String lastPart = parts[parts.length - 1];
            if (lastPart.startsWith(bundleName)) {
                return createPluginJavaConfig(resourceUrl.getPath());
            }
        }
        return null;
    }

    private PluginJavaConfig createPluginJavaConfig(final String bundleTestResourcePath) {

        return new PluginJavaConfig() {

            @Override
            public boolean equals(final Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }

                return getPluginKey().equals(((PluginJavaConfig) o).getPluginKey());
            }

            @Override
            public int hashCode() {
                return getPluginKey().hashCode();
            }

            @Override
            public int compareTo(final PluginConfig o) {
                return getPluginKey().compareTo(o.getPluginKey());
            }

            @Override
            public String getBundleJarPath() {
                return bundleTestResourcePath;
            }

            @Override
            public String getPluginKey() {
                return "key";
            }

            @Override
            public String getPluginName() {
                return bundleName;
            }

            @Override
            public PluginType getPluginType() {
                return PluginType.PAYMENT;
            }

            @Override
            public String getVersion() {
                return killbillVersion;
            }

            @Override
            public String getPluginVersionnedName() {
                return bundleName + "-" + killbillVersion;
            }

            @Override
            public File getPluginVersionRoot() {
                final StringBuilder tmp = new StringBuilder(rootInstallDir.getAbsolutePath());
                tmp.append("/plugins/")
                   .append(PluginLanguage.JAVA.toString().toLowerCase())
                   .append("/")
                   .append(bundleName)
                   .append("/")
                   .append(killbillVersion);
                return new File(tmp.toString());
            }

            @Override
            public PluginLanguage getPluginLanguage() {
                return PluginLanguage.JAVA;
            }

            @Override
            public boolean isSelectedForStart() {
                return true;
            }

            @Override
            public boolean isDisabled() {
                return false;
            }
        };
    }

    private static void unTar(final File inputFile, final File outputDir) throws IOException, ArchiveException {
        InputStream is = null;
        TarArchiveInputStream archiveInputStream = null;
        TarArchiveEntry entry;

        try {
            is = new FileInputStream(inputFile);
            archiveInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is);
            while ((entry = (TarArchiveEntry) archiveInputStream.getNextEntry()) != null) {
                final File outputFile = new File(outputDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!outputFile.exists()) {
                        if (!outputFile.mkdirs()) {
                            throw new IllegalStateException(String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()));
                        }
                    }
                } else {
                    final OutputStream outputFileStream = new FileOutputStream(outputFile);
                    ByteStreams.copy(archiveInputStream, outputFileStream);
                    outputFileStream.close();
                }
            }
        } finally {
            if (archiveInputStream != null) {
                archiveInputStream.close();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    private static File unGzip(final File inputFile, final File outputDir) throws IOException {
        final File outputFile = new File(outputDir, inputFile.getName().substring(0, inputFile.getName().length() - 3));

        try (final GZIPInputStream in = new GZIPInputStream(new FileInputStream(inputFile));
             final FileOutputStream out = new FileOutputStream(outputFile)) {
            for (int c = in.read(); c != -1; c = in.read()) {
                out.write(c);
            }
            return outputFile;
        }
    }
}
