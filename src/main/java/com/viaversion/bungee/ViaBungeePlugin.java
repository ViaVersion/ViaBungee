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

import java.io.File;
import java.net.MalformedURLException;
import net.lenni0451.reflect.ClassLoaders;
import net.md_5.bungee.api.plugin.Plugin;

public class ViaBungeePlugin extends Plugin {

    private ViaBungeePlatform platform;

    @Override
    public void onLoad() {
        getDataFolder().mkdirs();
        loadImplementation();
        platform = new ViaBungeePlatform(this, getDataFolder());
    }

    @Override
    public void onEnable() {
        platform.onEnable();
    }

    private void loadImplementation() {
        final File[] files = getDataFolder().listFiles();
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("You need to place the main ViaVersion jar in plugins/ViaVersion/");
        }

        boolean found = false;
        try {
            for (final File file : files) {
                if (file.getName().endsWith(".jar")) {
                    ClassLoaders.loadToFront(file.toURI().toURL());
                    found = true;
                }
            }
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }

        if (!found) {
            throw new IllegalArgumentException("You need to place the main ViaVersion jar in plugins/ViaVersion/");
        }
    }
}
