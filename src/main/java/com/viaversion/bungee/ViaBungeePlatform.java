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

package com.viaversion.bungee;

import com.google.common.collect.ImmutableList;
import com.viaversion.bungee.commands.BungeeCommand;
import com.viaversion.bungee.commands.BungeeCommandHandler;
import com.viaversion.bungee.commands.BungeeCommandSender;
import com.viaversion.bungee.platform.BungeeViaAPI;
import com.viaversion.bungee.platform.BungeeViaConfig;
import com.viaversion.bungee.platform.BungeeViaInjector;
import com.viaversion.bungee.platform.BungeeViaLoader;
import com.viaversion.bungee.platform.BungeeViaTask;
import com.viaversion.bungee.service.ProtocolDetectorService;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import com.viaversion.viaversion.api.command.ViaCommandSender;
import com.viaversion.viaversion.api.platform.PlatformTask;
import com.viaversion.viaversion.api.platform.UnsupportedSoftware;
import com.viaversion.viaversion.api.platform.ViaServerProxyPlatform;
import com.viaversion.viaversion.dump.PluginInfo;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.unsupported.UnsupportedServerSoftware;
import com.viaversion.viaversion.util.GsonUtil;
import com.viaversion.viaversion.util.VersionInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.protocol.ProtocolConstants;

public final class ViaBungeePlatform implements ViaServerProxyPlatform<ProxiedPlayer> {
    private final ProtocolDetectorService protocolDetectorService = new ProtocolDetectorService();
    private final ViaBungeePlugin plugin;
    private final BungeeViaAPI api;
    private final BungeeViaConfig viaConfig;
    private final ViaBungeeConfig bungeeConfig;

    public ViaBungeePlatform(final ViaBungeePlugin plugin, final File pluginFolder) {
        this.plugin = plugin;
        try {
            ProtocolConstants.class.getField("MINECRAFT_1_21");
        } catch (final NoSuchFieldException e) {
            getLogger().warning("      / \\");
            getLogger().warning("     /   \\");
            getLogger().warning("    /  |  \\");
            getLogger().warning("   /   |   \\         BUNGEECORD IS OUTDATED");
            getLogger().warning("  /         \\   VIAVERSION MAY NOT WORK AS INTENDED");
            getLogger().warning(" /     o     \\");
            getLogger().warning("/_____________\\");
        }

        getLogger().warning("ViaVersion does not work as intended across many different server versions, especially the more recent ones. " +
            "Consider moving Via plugins to your backend server or switching to Velocity.");

        api = new BungeeViaAPI();
        viaConfig = new BungeeViaConfig(getDataFolder(), getLogger());
        bungeeConfig = new ViaBungeeConfig(new File(pluginFolder, "config.yml"), getLogger());
        BungeeCommandHandler commandHandler = new BungeeCommandHandler();
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new BungeeCommand(commandHandler));

        // Init platform
        Via.init(ViaManagerImpl.builder()
            .platform(this)
            .injector(new BungeeViaInjector())
            .loader(new BungeeViaLoader(this))
            .commandHandler(commandHandler)
            .build());

        viaConfig.reload();
        bungeeConfig.reload();

        if (hasClass("com.viaversion.viabackwards.api.ViaBackwardsPlatform")) {
            getLogger().info("Found ViaBackwards, loading it");
            Via.getManager().addEnableListener(() -> new ViaBackwardsLoader(getLogger(), getDataFolder()));
        }
        if (hasClass("com.viaversion.viarewind.api.ViaRewindPlatform")) {
            getLogger().info("Found ViaRewind, loading it");
            Via.getManager().addEnableListener(() -> new ViaRewindLoader(getLogger(), getDataFolder()));
        }
        if (hasClass("net.raphimc.viaaprilfools.platform.ViaAprilFoolsPlatform")) {
            getLogger().info("Found ViaAprilFools, loading it");
            Via.getManager().addEnableListener(() -> new ViaAprilFoolsLoader(getLogger(), getDataFolder()));
        }
    }

    private boolean hasClass(final String name) {
        try {
            Class.forName(name);
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }

    public void onEnable() {
        final ViaManagerImpl manager = (ViaManagerImpl) Via.getManager();
        manager.init();
        manager.onServerLoaded();
    }

    @Override
    public Logger getLogger() {
        return plugin.getLogger();
    }

    public ProxyServer getProxy() {
        return plugin.getProxy();
    }

    @Override
    public String getPlatformName() {
        return getProxy().getName();
    }

    @Override
    public String getPlatformVersion() {
        return getProxy().getVersion();
    }

    @Override
    public boolean isProxy() {
        return true;
    }

    @Override
    public String getPluginVersion() {
        // Return the implementation version for dumps and logs
        return VersionInfo.getVersion();
    }

    @Override
    public PlatformTask runAsync(Runnable runnable) {
        return new BungeeViaTask(getProxy().getScheduler().runAsync(plugin, runnable));
    }

    @Override
    public PlatformTask runRepeatingAsync(final Runnable runnable, final long ticks) {
        return new BungeeViaTask(getProxy().getScheduler().schedule(plugin, runnable, 0, ticks * 50, TimeUnit.MILLISECONDS));
    }

    @Override
    public PlatformTask runSync(Runnable runnable) {
        return runAsync(runnable);
    }

    @Override
    public PlatformTask runSync(Runnable runnable, long delay) {
        return new BungeeViaTask(getProxy().getScheduler().schedule(plugin, runnable, delay * 50, TimeUnit.MILLISECONDS));
    }

    @Override
    public PlatformTask runRepeatingSync(Runnable runnable, long period) {
        return runRepeatingAsync(runnable, period);
    }

    @Override
    public ViaCommandSender[] getOnlinePlayers() {
        Collection<ProxiedPlayer> players = getProxy().getPlayers();
        ViaCommandSender[] array = new ViaCommandSender[players.size()];
        int i = 0;
        for (ProxiedPlayer player : players) {
            array[i++] = new BungeeCommandSender(player);
        }
        return array;
    }

    @Override
    public void sendMessage(UUID uuid, String message) {
        getProxy().getPlayer(uuid).sendMessage(message);
    }

    @Override
    public boolean kickPlayer(UUID uuid, String message) {
        ProxiedPlayer player = getProxy().getPlayer(uuid);
        if (player != null) {
            player.disconnect(message);
            return true;
        }
        return false;
    }

    @Override
    public boolean isPluginEnabled() {
        return true;
    }

    @Override
    public ViaAPI<ProxiedPlayer> getApi() {
        return api;
    }

    @Override
    public BungeeViaConfig getConf() {
        return viaConfig;
    }

    public ViaBungeeConfig getBungeeConfig() {
        return bungeeConfig;
    }

    @Override
    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public void onReload() {
        // Injector prints a message <3
    }

    @Override
    public JsonObject getDump() {
        JsonObject platformSpecific = new JsonObject();

        List<PluginInfo> plugins = new ArrayList<>();
        for (Plugin p : ProxyServer.getInstance().getPluginManager().getPlugins())
            plugins.add(new PluginInfo(
                true,
                p.getDescription().getName(),
                p.getDescription().getVersion(),
                p.getDescription().getMain(),
                Collections.singletonList(p.getDescription().getAuthor())
            ));

        platformSpecific.add("plugins", GsonUtil.getGson().toJsonTree(plugins));
        platformSpecific.add("servers", GsonUtil.getGson().toJsonTree(protocolDetectorService.detectedProtocolVersions()));
        return platformSpecific;
    }

    @Override
    public Collection<UnsupportedSoftware> getUnsupportedSoftwareClasses() {
        final Collection<UnsupportedSoftware> list = new ArrayList<>(ViaServerProxyPlatform.super.getUnsupportedSoftwareClasses());
        list.add(new UnsupportedServerSoftware.Builder()
            .name("FlameCord")
            .addClassName("dev._2lstudios.flamecord.FlameCord")
            .reason(UnsupportedServerSoftware.Reason.BREAKING_PROXY_SOFTWARE)
            .build());
        return ImmutableList.copyOf(list);
    }

    @Override
    public boolean hasPlugin(final String name) {
        return getProxy().getPluginManager().getPlugin(name) != null;
    }

    @Override
    public ProtocolDetectorService protocolDetectorService() {
        return protocolDetectorService;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
