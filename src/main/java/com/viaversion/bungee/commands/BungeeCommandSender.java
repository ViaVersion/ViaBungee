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
package com.viaversion.bungee.commands;

import com.viaversion.viaversion.api.command.ViaCommandSender;
import java.util.UUID;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public record BungeeCommandSender(CommandSender sender) implements ViaCommandSender {

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    public void sendMessage(String msg) {
        sender.sendMessage(msg);
    }

    @Override
    public UUID getUUID() {
        if (sender instanceof ProxiedPlayer player) {
            return player.getUniqueId();
        } else {
            return new UUID(0, 0);
        }
    }

    @Override
    public String getName() {
        return sender.getName();
    }
}
