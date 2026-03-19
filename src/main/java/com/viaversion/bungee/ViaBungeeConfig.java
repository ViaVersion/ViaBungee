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
package com.viaversion.bungee;

import com.viaversion.viaversion.util.Config;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class ViaBungeeConfig extends Config {

    private int bungeePingInterval;
    private boolean bungeePingSave;
    private Map<String, Integer> bungeeServerProtocols;

    ViaBungeeConfig(final File configFile, final Logger logger) {
        super(configFile, logger);
    }

    @Override
    public void reload() {
        super.reload();

        bungeePingInterval = getInt("bungee-ping-interval", 60);
        bungeePingSave = getBoolean("bungee-ping-save", true);
        bungeeServerProtocols = get("bungee-servers", new HashMap<>());
    }

    @Override
    protected void handleConfig(final Map<String, Object> config) {
    }

    @Override
    public URL getDefaultConfigURL() {
        return getClass().getClassLoader().getResource("assets/viabungee/config.yml");
    }

    @Override
    public InputStream getDefaultConfigInputStream() {
        return getClass().getClassLoader().getResourceAsStream("assets/viabungee/config.yml");
    }

    @Override
    public List<String> getUnsupportedOptions() {
        return List.of();
    }

    /**
     * What is the interval for checking servers via ping
     * -1 for disabled
     *
     * @return Ping interval in seconds
     */
    public int getBungeePingInterval() {
        return bungeePingInterval;
    }

    /**
     * Should the bungee ping be saved to the config on change.
     *
     * @return True if it should save
     */
    public boolean isBungeePingSave() {
        return bungeePingSave;
    }

    /**
     * Get the listed server protocols in the config.
     * default will be listed as default.
     *
     * @return Map of String, Integer
     */
    public Map<String, Integer> getBungeeServerProtocols() {
        return bungeeServerProtocols;
    }
}
