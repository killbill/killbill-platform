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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.killbill.billing.osgi.config.OSGIConfig;
import org.killbill.billing.osgi.pluginconf.PluginConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

@Singleton
public class PureOSGIBundleFinder {

    private final Logger logger = LoggerFactory.getLogger(Logger.class);

    private final OSGIConfig osgiConfig;

    // Pure OSGI bundle use their OSGI symbolicName as a pluginName. In order to provide a way to restart them
    // we need to keep a mapping between the pluginName (used for the api call) and the path on the filesystem
    private final Map<String, String> osgiPluginNameMapping;

    @Inject
    public PureOSGIBundleFinder(final OSGIConfig osgiConfig) {
        this.osgiConfig = osgiConfig;
        this.osgiPluginNameMapping = new HashMap<String, String>();
    }

    public List<String> getLatestBundles() throws PluginConfigException {
        final String rootDirPath = getPlatformOSGIBundlesRootDir();
        final File rootDir = new File(rootDirPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            logger.warn("Configuration root dir {} is not a valid directory", rootDirPath);
            return ImmutableList.<String>of();
        }

        final File[] files = rootDir.listFiles();
        if (files == null) {
            return ImmutableList.<String>of();
        }

        final List<String> bundles = new ArrayList<String>();
        for (final File bundleJar : files) {
            if (bundleJar.isFile()) {
                bundles.add(bundleJar.getAbsolutePath());
            }
        }

        return bundles;
    }

    public String getOSGIPath(final String pluginName) {
        return osgiPluginNameMapping.get(pluginName);
    }

    // Called when the system initialize itself and instantiate the OSGI bundle for the first time.
    public void recordMappingPluginNameToPath(final String pluginName, final String path) {
        osgiPluginNameMapping.put(pluginName, path);
    }

    public String getPlatformOSGIBundlesRootDir() {
        return osgiConfig.getRootInstallationDir() + "/platform/";
    }
}
