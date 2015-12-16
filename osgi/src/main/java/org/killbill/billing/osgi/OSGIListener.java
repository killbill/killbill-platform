/*
 * Copyright 2015 Groupon, Inc
 * Copyright 2015 The Billing Project, LLC
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

import java.io.IOException;

import javax.inject.Inject;

import org.killbill.billing.notification.plugin.api.BroadcastMetadata;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.osgi.BundleRegistry.BundleWithMetadata;
import org.killbill.billing.osgi.api.DefaultPluginsInfoApi;
import org.killbill.billing.osgi.api.DefaultPluginsInfoApi.DefaultPluginInfo;
import org.killbill.billing.osgi.api.PluginInfo;
import org.killbill.billing.osgi.api.PluginServiceInfo;
import org.killbill.billing.osgi.pluginconf.PluginFinder;
import org.killbill.billing.util.nodes.DefaultNodeCommandMetadata;
import org.killbill.billing.util.nodes.KillbillNodesApi;
import org.killbill.billing.util.nodes.NodeCommandMetadata;
import org.killbill.billing.util.nodes.PluginNodeCommandMetadata;
import org.killbill.billing.util.nodes.SystemNodeCommandType;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

public class OSGIListener {

    private static final Logger logger = LoggerFactory.getLogger(OSGIListener.class);

    private final ObjectMapper objectMapper;
    private final BundleRegistry bundleRegistry;

    private final PluginFinder pluginFinder;
    private final KillbillNodesApi nodesApi;

    @Inject
    public OSGIListener(final BundleRegistry bundleRegistry, final PluginFinder pluginFinder, final KillbillNodesApi nodesApi) {
        this.bundleRegistry = bundleRegistry;
        this.nodesApi = nodesApi;
        this.pluginFinder = pluginFinder;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @AllowConcurrentEvents
    @Subscribe
    public void handleKillbillEvent(final ExtBusEvent event) throws IOException, BundleException {
        if (event.getEventType() != ExtBusEventType.BROADCAST_SERVICE) {
            return;
        }

        final BroadcastMetadata metadata = objectMapper.readValue(event.getMetaData(), BroadcastMetadata.class);
        final SystemNodeCommandType commandType = getSystemNodeCommandTypeOrNull(metadata.getCommandType());
        if (commandType == null || (commandType != SystemNodeCommandType.START_PLUGIN &&
                                    commandType != SystemNodeCommandType.STOP_PLUGIN &&
                                    commandType != SystemNodeCommandType.RESTART_PLUGIN)) {
            return;
        }

        final PluginNodeCommandMetadata nodeCommandMetadata = (PluginNodeCommandMetadata) deserializeNodeCommand(metadata.getEventJson(), metadata.getCommandType());

        final String pluginName = nodeCommandMetadata.getPluginName() != null ?
                                  nodeCommandMetadata.getPluginName() :
                                  (pluginFinder.resolvePluginKey(nodeCommandMetadata.getPluginKey()) != null ? pluginFinder.resolvePluginKey(nodeCommandMetadata.getPluginKey()).getPluginName() : null);
        if (pluginName == null) {
            logger.warn(String.format("Failed to dispatch event %s : Input must have a pluginName or a valid pluginKey specified json=%s", commandType, metadata.getEventJson()));
            return;
        }

        BundleWithMetadata bundleWithMetadata = null;
        switch(commandType) {
            case STOP_PLUGIN:
                bundleRegistry.stopAndUninstallNewBundle(pluginName, nodeCommandMetadata.getPluginVersion());
                break;
            case START_PLUGIN:
                bundleWithMetadata = bundleRegistry.installAndStartNewBundle(pluginName, nodeCommandMetadata.getPluginVersion());
                break;
            case RESTART_PLUGIN:
                bundleRegistry.stopAndUninstallNewBundle(pluginName, nodeCommandMetadata.getPluginVersion());
                bundleWithMetadata = bundleRegistry.installAndStartNewBundle(pluginName, nodeCommandMetadata.getPluginVersion());
                break;
            default:
                throw new IllegalStateException("Unexpected type " + commandType);
        }

        final String defaultPluginVersion = pluginFinder.getPluginVersionSelectedForStart(nodeCommandMetadata.getPluginName());
        boolean isSelectedForStart = defaultPluginVersion != null && nodeCommandMetadata.getPluginVersion() != null ? defaultPluginVersion.equals(nodeCommandMetadata.getPluginVersion()) : true; /* this is lie, we don't know */

        final String symbolicName = (bundleWithMetadata != null &&  bundleWithMetadata.getBundle() != null) ? bundleWithMetadata.getBundle().getSymbolicName() : null;
        final PluginInfo pluginInfo = new DefaultPluginInfo(nodeCommandMetadata.getPluginKey(), symbolicName, nodeCommandMetadata.getPluginName(), nodeCommandMetadata.getPluginVersion(), DefaultPluginsInfoApi.toPluginState(bundleWithMetadata), isSelectedForStart, ImmutableSet.<PluginServiceInfo>of());
        nodesApi.notifyPluginChanged(pluginInfo);
    }

    private SystemNodeCommandType getSystemNodeCommandTypeOrNull(final String command) {
        return Iterables.tryFind(ImmutableList.copyOf(SystemNodeCommandType.values()), new Predicate<SystemNodeCommandType>() {
            @Override
            public boolean apply(final SystemNodeCommandType input) {
                return input.name().equals(command);
            }
        }).orNull();
    }

    private NodeCommandMetadata deserializeNodeCommand(final String nodeCommand, final String type) throws IOException {

        final SystemNodeCommandType systemType = Iterables.tryFind(ImmutableList.copyOf(SystemNodeCommandType.values()), new Predicate<SystemNodeCommandType>() {
            @Override
            public boolean apply(final SystemNodeCommandType input) {
                return input.name().equals(type);
            }
        }).orNull();
        return (systemType != null) ?
               objectMapper.readValue(nodeCommand, systemType.getCommandMetadataClass()) :
               objectMapper.readValue(nodeCommand, DefaultNodeCommandMetadata.class);
    }


}
