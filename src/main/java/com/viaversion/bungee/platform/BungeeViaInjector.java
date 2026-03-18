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

import com.viaversion.bungee.handlers.BungeeDecodeHandler;
import com.viaversion.bungee.handlers.BungeeEncodeHandler;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.libs.fastutil.objects.ObjectLinkedOpenHashSet;
import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import com.viaversion.viaversion.util.ReflectionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.channel.BungeeChannelInitializer;
import net.md_5.bungee.protocol.packet.SetCompression;

import static com.viaversion.bungee.handlers.PipelineConstants.PACKET_DECODER;
import static com.viaversion.bungee.handlers.PipelineConstants.PACKET_ENCODER;
import static com.viaversion.bungee.handlers.PipelineConstants.VIA_DECODER;
import static com.viaversion.bungee.handlers.PipelineConstants.VIA_ENCODER;

public class BungeeViaInjector implements ViaInjector {

    private final List<Channel> injectedChannels = new ArrayList<>();

    @Override
    public void inject() {
        final ProxyServer.Unsafe unsafe = ProxyServer.getInstance().unsafe();
        final BungeeChannelInitializer frontendConnection = unsafe.getFrontendChannelInitializer();
        final BungeeChannelInitializer backendConnection = unsafe.getBackendChannelInitializer();

        unsafe.setFrontendChannelInitializer(BungeeChannelInitializer.create(channel -> {
            final boolean accepted = frontendConnection.getChannelAcceptor().accept(channel);
            if (accepted) {
                injectedChannels.add(channel);
                injectPipeline(channel, false);
            }
            return accepted;
        }));
        unsafe.setBackendChannelInitializer(BungeeChannelInitializer.create(channel -> {
            final boolean accepted = backendConnection.getChannelAcceptor().accept(channel);
            if (accepted) {
                injectedChannels.add(channel);
                injectPipeline(channel, true);
            }
            return accepted;
        }));
        Via.getPlatform().getLogger().info("Successfully injected ViaVersion into BungeeCord!");
    }

    private void injectPipeline(final Channel channel, final boolean clientside) {
        final UserConnection connection = new UserConnectionImpl(channel, clientside);
        new ProtocolPipelineImpl(connection);

        final BungeeDecodeHandler decode = new BungeeDecodeHandler(connection);
        final BungeeEncodeHandler encode = new BungeeEncodeHandler(connection);

        injectDecoder(channel, decode);
        channel.pipeline().addBefore(PACKET_ENCODER, VIA_ENCODER, encode);

        channel.pipeline().addAfter(PACKET_ENCODER, "via-encode-reorder", new ChannelOutboundHandlerAdapter() {

            @Override
            public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
                if (msg instanceof SetCompression) {
                    channel.eventLoop().execute(() -> injectDecoder(channel, decode));
                }
                super.write(ctx, msg, promise);
            }
        });

        channel.pipeline().addAfter(PACKET_DECODER, "via-decode-reorder", new ChannelInboundHandlerAdapter() {

            @Override
            public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
                super.channelRead(ctx, msg);
                if (msg instanceof PacketWrapper packetWrapper && packetWrapper.packet instanceof SetCompression) {
                    injectDecoder(channel, decode);
                }
            }
        });
    }

    private void injectDecoder(final Channel channel, final BungeeDecodeHandler handler) {
        if (channel.pipeline().get(VIA_DECODER) != null) {
            channel.pipeline().remove(VIA_DECODER);
        }
        channel.pipeline().addBefore(PACKET_DECODER, VIA_DECODER, handler);
    }

    @Override
    public void uninject() {
        Via.getPlatform().getLogger().severe("ViaVersion cannot remove itself from Bungee without a reboot!");
    }

    @Override
    public ProtocolVersion getServerProtocolVersion() {
        return ProtocolVersion.getProtocol(ProtocolConstants.SUPPORTED_VERSION_IDS.get(0));
    }

    @Override
    public SortedSet<ProtocolVersion> getServerProtocolVersions() {
        final SortedSet<ProtocolVersion> versions = new ObjectLinkedOpenHashSet<>();
        for (final Integer version : ProtocolConstants.SUPPORTED_VERSION_IDS) {
            versions.add(ProtocolVersion.getProtocol(version));
        }
        return versions;
    }

    @Override
    public JsonObject getDump() {
        JsonObject data = new JsonObject();

        // Generate information about current injections
        JsonArray injectedChannelInitializers = new JsonArray();
        for (Channel channel : this.injectedChannels) {
            JsonObject channelInfo = new JsonObject();
            channelInfo.addProperty("channelClass", channel.getClass().getName());

            // Get information about the pipes for this channel
            JsonArray pipeline = new JsonArray();
            for (String pipeName : channel.pipeline().names()) {
                JsonObject handlerInfo = new JsonObject();
                handlerInfo.addProperty("name", pipeName);

                ChannelHandler channelHandler = channel.pipeline().get(pipeName);
                if (channelHandler == null) {
                    handlerInfo.addProperty("status", "INVALID");
                    continue;
                }

                handlerInfo.addProperty("class", channelHandler.getClass().getName());

                try {
                    Object child = ReflectionUtil.get(channelHandler, "childHandler", ChannelInitializer.class);
                    handlerInfo.addProperty("childClass", child.getClass().getName());
                } catch (ReflectiveOperationException e) {
                    // Don't display
                }

                pipeline.add(handlerInfo);
            }
            channelInfo.add("pipeline", pipeline);

            injectedChannelInitializers.add(channelInfo);
        }

        data.add("injectedChannelInitializers", injectedChannelInitializers);

        return data;
    }
}
