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
package com.viaversion.bungee.platform;

import com.viaversion.viaversion.configuration.AbstractViaConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class BungeeViaConfig extends AbstractViaConfig {

    private final List<String> UNSUPPORTED = new ArrayList<>();

    public BungeeViaConfig(File folder, Logger logger) {
        super(new File(folder, "viaversion.yml"), logger);

        UNSUPPORTED.addAll(AbstractViaConfig.BUKKIT_ONLY_OPTIONS);
        UNSUPPORTED.addAll(AbstractViaConfig.VELOCITY_ONLY_OPTIONS);
    }

    @Override
    public List<String> getUnsupportedOptions() {
        return UNSUPPORTED;
    }
}
