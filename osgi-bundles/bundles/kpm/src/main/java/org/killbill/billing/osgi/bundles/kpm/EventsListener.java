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

package org.killbill.billing.osgi.bundles.kpm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.killbill.billing.notification.plugin.api.BroadcastMetadata;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.util.nodes.NodeCommandProperty;
import org.killbill.billing.util.nodes.PluginNodeCommandMetadata;
import org.killbill.commons.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventsListener implements OSGIKillbillEventDispatcher.OSGIKillbillEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(EventsListener.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // FIXME-TS-58: Remove @Deprecated and delete KPMWrapper once technical-support-58 done
    @Deprecated
    private final KPMWrapper kpmWrapper;

    private final PluginManager pluginManager;

    public EventsListener(final KPMWrapper kpmWrapper, final PluginManager pluginManager) {
        this.kpmWrapper = kpmWrapper;
        this.pluginManager = pluginManager;
    }

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {
        if (killbillEvent.getEventType() != ExtBusEventType.BROADCAST_SERVICE) {
            return;
        }

        if (killbillEvent.getMetaData() == null) {
            logger.debug("Ignoring BROADCAST_SERVICE event without metadata: {}", killbillEvent);
            return;
        }

        final BroadcastMetadata broadcastMetadata;
        try {
            broadcastMetadata = objectMapper.readValue(killbillEvent.getMetaData(), new TypeReference<BroadcastMetadata>() {});
        } catch (final IOException e) {
            logger.warn("Ignoring BROADCAST_SERVICE event with invalid broadcastMetadata: {}", killbillEvent, e);
            return;
        }

        final String commandType = broadcastMetadata.getCommandType();
        if (!("INSTALL_PLUGIN".equals(commandType) || "UNINSTALL_PLUGIN".equals(commandType))) {
            logger.debug("Ignoring BROADCAST_SERVICE event: {}", killbillEvent);
            return;
        }

        final PluginNodeCommandMetadata nodeCommandMetadata;
        try {
            nodeCommandMetadata = objectMapper.readValue(broadcastMetadata.getEventJson(), new TypeReference<PluginNodeCommandMetadata>() {});
        } catch (final IOException e) {
            logger.warn("Ignoring BROADCAST_SERVICE event with invalid nodeCommandMetadata: {}", killbillEvent, e);
            return;
        }

        // Only required option
        if (Strings.isNullOrEmpty(nodeCommandMetadata.getPluginKey())) {
            logger.warn("Ignoring BROADCAST_SERVICE event with missing pluginKey: {}", killbillEvent);
            return;
        }

        if ("INSTALL_PLUGIN".equals(commandType)) {
            final Map<String, String> props = toMap(nodeCommandMetadata.getProperties());
            final String pluginType = props.get("pluginType");

            final String pluginUri = props.get("pluginUri");
            if (pluginUri != null) {
                try {
                    // FIXME-TS-58: Remove this comments block once TS-58 done
                    /*
                    kpmWrapper.install(nodeCommandMetadata.getPluginKey(),
                                       pluginUri,
                                       nodeCommandMetadata.getPluginVersion(),
                                       pluginType);*/
                    pluginManager.install(pluginUri, nodeCommandMetadata.getPluginKey(), nodeCommandMetadata.getPluginVersion());
                } catch (final Exception e) {
                    logger.warn("Unable to install plugin {}", nodeCommandMetadata.getPluginKey(), e);
                }
            } else {
                // Special property passed by the Kill Bill node which sent the broadcase
                final String kbVersion = Objects.requireNonNullElse(props.get("kbVersion"), "LATEST");
                final String pluginArtifactId = props.get("pluginArtifactId");
                final String pluginGroupId = props.get("pluginGroupId");
                final String pluginPackaging = props.get("pluginPackaging");
                final String pluginClassifier = props.get("pluginClassifier");
                final boolean forceDownload = "true".equals(props.get("forceDownload"));
                kpmWrapper.install(nodeCommandMetadata.getPluginKey(),
                                   kbVersion,
                                   pluginArtifactId,
                                   nodeCommandMetadata.getPluginVersion(),
                                   pluginGroupId,
                                   pluginPackaging,
                                   pluginClassifier,
                                   pluginType,
                                   forceDownload);
            }
        } else //noinspection ConstantConditions
            if ("UNINSTALL_PLUGIN".equals(commandType)) {
                kpmWrapper.uninstall(nodeCommandMetadata.getPluginKey(), nodeCommandMetadata.getPluginVersion());
            }
    }

    private Map<String, String> toMap(final Iterable<NodeCommandProperty> properties) {
        final Map<String, String> map = new HashMap<String, String>();
        for (final NodeCommandProperty property : properties) {
            if (property.getKey() != null && property.getValue() != null && !property.getValue().toString().isEmpty()) {
                map.put(property.getKey(), property.getValue().toString());
            }
        }
        return map;
    }
}
