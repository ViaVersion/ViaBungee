/*
 * This file is part of ViaBungee - https://github.com/ViaVersion/ViaBungee
 * Copyright (C) 2016-2026 ViaVersion and contributors
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
package com.viaversion.bungee.listeners;

import com.viaversion.bungee.ViaBungeePlatform;
import com.viaversion.bungee.storage.BungeeStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.connection.ConnectionDetails;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class ConnectionDetailsListener implements Listener {

    private final ViaBungeePlatform plugin;

    public ConnectionDetailsListener(ViaBungeePlatform plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent e) {
        final UUID uuid;
        // Bungee may rewrite the uuid sent in the login success packet to an offline UUID,
        // we need to manually find the correct uuid to lookup for, as ProxiedPlayer#getUniqueId will always return
        // the online/correct UUID.
        if (plugin.getProxy().getConfig().isIpForward()) {
            uuid = e.getPlayer().getUniqueId();
        } else {
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + e.getPlayer().getName()).getBytes(StandardCharsets.UTF_8));
        }

        final UserConnection connection = Via.getManager().getConnectionManager().getClientConnection(uuid);
        if (connection != null) {
            // Update tracked UUID to the actual one set on the server
            connection.getProtocolInfo().setUuid(e.getPlayer().getUniqueId());
            if (!connection.has(BungeeStorage.class)) {
                connection.put(new BungeeStorage(e.getServer()));
            }

            ConnectionDetails.sendConnectionDetails(connection, ConnectionDetails.PROXY_CHANNEL);
        }
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (e.getSender() instanceof ProxiedPlayer && e.getTag().equals(ConnectionDetails.PROXY_CHANNEL)) {
            e.setCancelled(true);
        }
    }
}
