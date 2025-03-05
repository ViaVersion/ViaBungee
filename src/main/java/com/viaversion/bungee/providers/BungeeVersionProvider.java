/*
 * This file is part of ViaBungee - https://github.com/ViaVersion/ViaBungee
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.bungee.providers;

import com.google.common.collect.Lists;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.protocol.version.BaseVersionProvider;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import io.netty.channel.Channel;
import net.md_5.bungee.protocol.ProtocolConstants;

public class BungeeVersionProvider extends BaseVersionProvider {
    private static final MethodHandle GET_SERVER_CONNECTOR;
    private static final MethodHandle GET_BUNGEE_SERVER_INFO;
    private static final MethodHandle GET_SERVER_NAME;

    static {
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final Class<?> handlerBossClass = Class.forName("net.md_5.bungee.netty.HandlerBoss");
            final Class<?> packetHandlerClass = Class.forName("net.md_5.bungee.netty.PacketHandler");
            final Class<?> serverConnectorClass = Class.forName("net.md_5.bungee.ServerConnector");
            final Class<?> bungeeServerInfoClass = Class.forName("net.md_5.bungee.BungeeServerInfo");

            GET_SERVER_CONNECTOR = MethodHandles.privateLookupIn(handlerBossClass, lookup).findGetter(handlerBossClass, "handler", packetHandlerClass);
            GET_BUNGEE_SERVER_INFO = MethodHandles.privateLookupIn(serverConnectorClass, lookup).findGetter(serverConnectorClass, "target", bungeeServerInfoClass);
            GET_SERVER_NAME = MethodHandles.privateLookupIn(bungeeServerInfoClass, lookup).findGetter(bungeeServerInfoClass, "name", String.class);
        } catch (final ReflectiveOperationException e) {
            Via.getPlatform().getLogger().severe("Error initializing BungeeVersionProvider, try updating BungeeCord or ViaVersion!");
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProtocolVersion getClosestServerProtocol(UserConnection user) throws Exception {
        if (user.isClientSide()) {
            return getBackProtocol(user);
        } else {
            return getFrontProtocol(user);
        }
    }

    private String getServerName(final Channel channel) {
        try {
            final Object serverConnector = GET_SERVER_CONNECTOR.invoke(channel.pipeline().get("inbound-boss"));
            final Object bungeeServerInfo = GET_BUNGEE_SERVER_INFO.invoke(serverConnector);
            return (String) GET_SERVER_NAME.invoke(bungeeServerInfo);
        } catch (final Throwable e) {
            Via.getPlatform().getLogger().severe("Error getting server name from BungeeCord!");
            return null;
        }
    }

    private ProtocolVersion getBackProtocol(UserConnection user) {
        final String serverName = getServerName(user.getChannel());
        return Via.proxyPlatform().protocolDetectorService().serverProtocolVersion(serverName);
    }

    private ProtocolVersion getFrontProtocol(UserConnection user) throws Exception {
        List<Integer> sorted = new ArrayList<>(ProtocolConstants.SUPPORTED_VERSION_IDS);
        Collections.sort(sorted);

        ProtocolInfo info = user.getProtocolInfo();

        // Bungee supports it
        final ProtocolVersion clientProtocolVersion = info.protocolVersion();
        if (new HashSet<>(sorted).contains(clientProtocolVersion.getVersion())) {
            return clientProtocolVersion;
        }

        // Older than bungee supports, get the lowest version
        if (clientProtocolVersion.getVersion() < sorted.get(0)) {
            return Via.getManager().getInjector().getServerProtocolVersion();
        }

        // Loop through all protocols to get the closest protocol id that bungee supports (and that viaversion does too)

        // TODO: This needs a better fix, i.e checking ProtocolRegistry to see if it would work.
        // This is more of a workaround for snapshot support by bungee.
        for (Integer protocol : Lists.reverse(sorted)) {
            if (clientProtocolVersion.getVersion() > protocol && ProtocolVersion.isRegistered(protocol)) {
                return ProtocolVersion.getProtocol(protocol);
            }
        }

        Via.getPlatform().getLogger().severe("Panic, no protocol id found for " + clientProtocolVersion);
        return clientProtocolVersion;
    }
}
