/*
 * This file is part of ViaBungee - https://github.com/ViaVersion/ViaBungee
 * Copyright (C) 2016-2024 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.bungee.platform;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.bungee.providers.BungeeVersionProvider;
import com.viaversion.viaversion.configuration.AbstractViaConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BungeeViaConfig extends AbstractViaConfig {
    protected final List<String> UNSUPPORTED = new ArrayList<>();

    public BungeeViaConfig(File folder, Logger logger) {
        super(new File(folder, "viaversion.yml"), logger);

        UNSUPPORTED.addAll(AbstractViaConfig.BUKKIT_ONLY_OPTIONS);
        UNSUPPORTED.addAll(AbstractViaConfig.VELOCITY_ONLY_OPTIONS);
    }

    @Override
    protected void handleConfig(Map<String, Object> config) {
        // Parse servers
        Map<String, Object> servers;
        if (!(config.get("bungee-servers") instanceof Map)) {
            servers = new HashMap<>();
        } else {
            servers = (Map) config.get("bungee-servers");
        }
        // Convert any bad Protocol Ids
        for (Map.Entry<String, Object> entry : new HashSet<>(servers.entrySet())) {
            if (!(entry.getValue() instanceof Integer)) {
                if (entry.getValue() instanceof String stringValue) {
                    ProtocolVersion found = ProtocolVersion.getClosest(stringValue);
                    if (found != null) {
                        servers.put(entry.getKey(), found.getVersion());
                    } else {
                        servers.remove(entry.getKey()); // Remove!
                    }
                } else {
                    servers.remove(entry.getKey()); // Remove!
                }
            }
        }
        // Ensure default exists
        if (!servers.containsKey("default")) {
            servers.put("default", BungeeVersionProvider.getLowestSupportedVersion().getVersion());
        }
        // Put back
        config.put("bungee-servers", servers);
    }

    @Override
    public List<String> getUnsupportedOptions() {
        return UNSUPPORTED;
    }
}
