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
package com.viaversion.bungee.platform;

import com.viaversion.bungee.ViaBungeePlatform;
import com.viaversion.bungee.listeners.ElytraPatch;
import com.viaversion.bungee.listeners.UpdateListener;
import com.viaversion.bungee.providers.BungeeBossBarProvider;
import com.viaversion.bungee.providers.BungeeEntityIdProvider;
import com.viaversion.bungee.providers.BungeeMainHandProvider;
import com.viaversion.bungee.providers.BungeeVersionProvider;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocols.v1_8to1_9.provider.BossBarProvider;
import com.viaversion.viaversion.protocols.v1_8to1_9.provider.EntityIdProvider;
import com.viaversion.viaversion.protocols.v1_8to1_9.provider.MainHandProvider;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class BungeeViaLoader implements ViaPlatformLoader {
    private final Set<Listener> listeners = new HashSet<>();
    private final Set<ScheduledTask> tasks = new HashSet<>();
    private final ViaBungeePlatform plugin;

    public BungeeViaLoader(ViaBungeePlatform plugin) {
        this.plugin = plugin;
    }

    private void registerListener(Listener listener) {
        listeners.add(listener);
        ProxyServer.getInstance().getPluginManager().registerListener(plugin.getPlugin(), listener);
    }

    @Override
    public void load() {
        // Listeners
        registerListener(new UpdateListener());

        final ProtocolVersion protocolVersion = Via.getAPI().getServerVersion().lowestSupportedProtocolVersion();
        if (protocolVersion.olderThan(ProtocolVersion.v1_9)) {
            registerListener(new ElytraPatch());
        }

        // Providers
        Via.getManager().getProviders().use(VersionProvider.class, new BungeeVersionProvider());
        Via.getManager().getProviders().use(EntityIdProvider.class, new BungeeEntityIdProvider());

        if (protocolVersion.olderThan(ProtocolVersion.v1_9)) {
            Via.getManager().getProviders().use(BossBarProvider.class, new BungeeBossBarProvider());
            Via.getManager().getProviders().use(MainHandProvider.class, new BungeeMainHandProvider());
        }

        if (plugin.getBungeeConfig().getBungeePingInterval() > 0) {
            tasks.add(plugin.getProxy().getScheduler().schedule(
                plugin.getPlugin(),
                () -> Via.proxyPlatform().protocolDetectorService().probeAllServers(),
                0, plugin.getBungeeConfig().getBungeePingInterval(),
                TimeUnit.SECONDS
            ));
        }
    }

    @Override
    public void unload() {
        for (Listener listener : listeners) {
            ProxyServer.getInstance().getPluginManager().unregisterListener(listener);
        }
        listeners.clear();
        for (ScheduledTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
    }
}
