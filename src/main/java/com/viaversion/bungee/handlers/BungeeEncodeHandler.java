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

import com.viaversion.bungee.util.BungeePipelineUtil;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.exception.CancelCodecException;
import com.viaversion.viaversion.exception.CancelEncoderException;
import com.viaversion.viaversion.util.PipelineUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;

import static com.viaversion.bungee.handlers.PipelineConstants.*;

@ChannelHandler.Sharable
public class BungeeEncodeHandler extends MessageToMessageEncoder<ByteBuf> {

    private final UserConnection connection;
    private boolean handledCompression;

    public BungeeEncodeHandler(UserConnection connection) {
        this.connection = connection;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> out) {
        if (!connection.checkClientboundPacket()) {
            throw CancelEncoderException.generate(null);
        }
        if (!connection.shouldTransformPacket()) {
            out.add(bytebuf.retain());
            return;
        }

        ByteBuf transformedBuf = ctx.alloc().buffer().writeBytes(bytebuf);
        try {
            boolean needsCompress = handleCompressionOrder(ctx, transformedBuf);
            connection.transformClientbound(transformedBuf, CancelEncoderException::generate);
            if (needsCompress) {
                recompress(ctx, transformedBuf);
            }
            out.add(transformedBuf.retain());
        } finally {
            transformedBuf.release();
        }
    }

    private boolean handleCompressionOrder(ChannelHandlerContext ctx, ByteBuf buf) {
        if (handledCompression) {
            return false;
        }

        if (ctx.pipeline().names().indexOf(COMPRESS) > ctx.pipeline().names().indexOf(VIA_ENCODER)) {
            // Need to decompress this packet due to bad order
            ByteBuf decompressed = BungeePipelineUtil.decompress(ctx, buf);

            // Ensure the buffer wasn't reused
            if (buf != decompressed) {
                try {
                    buf.clear().writeBytes(decompressed);
                } finally {
                    decompressed.release();
                }
            }

            // Reorder the pipeline
            ChannelHandler decoder = ctx.pipeline().get(VIA_DECODER);
            ChannelHandler encoder = ctx.pipeline().get(VIA_ENCODER);
            ctx.pipeline().remove(decoder);
            ctx.pipeline().remove(encoder);
            ctx.pipeline().addAfter(DECOMPRESS, VIA_DECODER, decoder);
            ctx.pipeline().addAfter(COMPRESS, VIA_ENCODER, encoder);
            handledCompression = true;
            return true;
        }
        return false;
    }

    private void recompress(ChannelHandlerContext ctx, ByteBuf buf) {
        ByteBuf compressed = BungeePipelineUtil.compress(ctx, buf);
        try {
            buf.clear().writeBytes(compressed);
        } finally {
            compressed.release();
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        try {
            super.write(ctx, msg, promise);
        } catch (Throwable e) {
            if (!PipelineUtil.containsCause(e, CancelCodecException.class)) {
                throw e;
            } else {
                promise.setSuccess();
            }
        }
    }

    public UserConnection connection() {
        return connection;
    }
}
