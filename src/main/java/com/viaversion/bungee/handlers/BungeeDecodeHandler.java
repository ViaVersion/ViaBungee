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
package com.viaversion.bungee.handlers;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.exception.CancelCodecException;
import com.viaversion.viaversion.exception.CancelDecoderException;
import com.viaversion.viaversion.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

@ChannelHandler.Sharable
public class BungeeDecodeHandler extends MessageToMessageDecoder<ByteBuf> {

    private final UserConnection user;

    public BungeeDecodeHandler(UserConnection user) {
        this.user = user;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> out) {
        if (!user.checkServerboundPacket()) {
            throw CancelDecoderException.generate(null);
        }
        if (!user.shouldTransformPacket()) {
            out.add(bytebuf.retain());
            return;
        }

        ByteBuf transformedBuf = ByteBufUtil.copy(ctx.alloc(), bytebuf);
        try {
            user.transformServerbound(transformedBuf, CancelDecoderException::generate);
            out.add(transformedBuf.retain());
        } finally {
            transformedBuf.release();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            super.channelRead(ctx, msg);
        } catch (Throwable e) {
            if (!PipelineUtil.containsCause(e, CancelCodecException.class)) {
                throw e;
            }
        }
    }
}
