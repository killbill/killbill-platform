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

package org.killbill.billing.osgi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.annotation.Nullable;

import org.killbill.billing.util.nodes.KillbillNodesApi;
import org.killbill.billing.util.nodes.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;

public class JRubyJarHelper {

    private static final Logger logger = LoggerFactory.getLogger(JRubyJarHelper.class);

    static final String POM_PROPERTIES = "META-INF/maven/org.kill-bill.billing/killbill-platform-osgi-bundles-jruby/pom.properties";

    private final KillbillNodesApi nodesApi;
    private final String jrubyJarPath;

    public JRubyJarHelper(final String platformOSGIBundlesRootDir, @Nullable final KillbillNodesApi nodesApi) {
        this.jrubyJarPath = platformOSGIBundlesRootDir + "jruby.jar";
        this.nodesApi = nodesApi;
    }

    public String getAndValidateJrubyJarPath() {
        if (new File(jrubyJarPath).isFile()) {
            logIfVersionMismatch();
            return jrubyJarPath;
        } else {
            logger.warn("Unable to find the JRuby bundle at {}, ruby plugins won't be started!", jrubyJarPath);
            return null;
        }
    }

    private void logIfVersionMismatch() {
        try {
            final NodeInfo nodeInfo = nodesApi == null ? null : nodesApi.getCurrentNodeInfo();
            if (nodeInfo == null) {
                logger.warn("Failed to get current NodeInfo to validate jruby.jar {}", jrubyJarPath);
                return;
            }
            final String platformVersion = nodeInfo.getPlatformVersion();

            final Properties properties = getProperties(jrubyJarPath, POM_PROPERTIES);
            final String jrubyJarVersion = extractVersion(properties);
            if (jrubyJarVersion == null) {
                logger.warn("Failed to extract jruby.jar version from file {}", jrubyJarPath);
                return;
            }
            if (!jrubyJarVersion.equals(platformVersion)) {
                logger.warn("Detected version mismatch between existing jruby.jar [version={}] and platform [version={}]", jrubyJarVersion, platformVersion);
                return;
            }
        } catch (final IOException e) {
            logger.warn("Failed to validate jruby.jar version:", e);
        }
    }

    private Properties getProperties(final String jarFileName, final String entryName) throws IOException {

        InputStream stream = null;
        Reader reader = null;
        try {
            final JarFile jarFile = new JarFile(jarFileName);
            final ZipEntry entry = jarFile.getEntry(entryName);
            stream = jarFile.getInputStream(entry);
            reader = new InputStreamReader(stream, Charsets.UTF_8);
            final Properties props = new Properties();
            props.load(reader);
            return props;
        } finally {
            IOException streamIOException = null;
            if (stream != null) {
                try {
                    stream.close();
                } catch (final IOException ioe) {
                    streamIOException = ioe;
                }
            }
            if (reader != null) {
                reader.close();
            }
            if (streamIOException != null) {
                throw streamIOException;
            }
        }
    }

    @VisibleForTesting
    String extractVersion(final Properties properties) {
        return (String) properties.get("version");
    }

}
