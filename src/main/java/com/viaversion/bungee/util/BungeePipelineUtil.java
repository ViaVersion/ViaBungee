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
package com.viaversion.bungee.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.viaversion.bungee.handlers.PipelineConstants.COMPRESS;
import static com.viaversion.bungee.handlers.PipelineConstants.DECOMPRESS;

public final class BungeePipelineUtil {
    private static final Method DECODE_MESSAGE_METHOD;
    private static final Method ENCODE_MESSAGE_METHOD; // Waterfall support
    private static final Method ENCODE_BYTE_METHOD;

    static {
        try {
            DECODE_MESSAGE_METHOD = MessageToMessageDecoder.class.getDeclaredMethod("decode", ChannelHandlerContext.class, Object.class, List.class);
            DECODE_MESSAGE_METHOD.setAccessible(true);
            ENCODE_MESSAGE_METHOD = MessageToMessageEncoder.class.getDeclaredMethod("encode", ChannelHandlerContext.class, Object.class, List.class);
            ENCODE_MESSAGE_METHOD.setAccessible(true);
            ENCODE_BYTE_METHOD = MessageToByteEncoder.class.getDeclaredMethod("encode", ChannelHandlerContext.class, Object.class, ByteBuf.class);
            ENCODE_BYTE_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Object> callDecodeMessage(MessageToMessageDecoder<?> decoder, ChannelHandlerContext ctx, ByteBuf input) throws InvocationTargetException {
        List<Object> output = new ArrayList<>();
        try {
            DECODE_MESSAGE_METHOD.invoke(decoder, ctx, input, output);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    private static ByteBuf callEncodeByteBuf(MessageToByteEncoder<?> encoder, ChannelHandlerContext ctx, ByteBuf input) throws InvocationTargetException {
        ByteBuf output = ctx.alloc().buffer();
        try {
            ENCODE_BYTE_METHOD.invoke(encoder, ctx, input, output);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    private static List<Object> callEncodeMessage(MessageToMessageEncoder<?> encoder, ChannelHandlerContext ctx, ByteBuf input) throws InvocationTargetException {
        List<Object> output = new ArrayList<>();
        try {
            ENCODE_MESSAGE_METHOD.invoke(encoder, ctx, input, output);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    public static ByteBuf decompress(ChannelHandlerContext ctx, ByteBuf bytebuf) {
        final MessageToMessageDecoder<?> decompressor = (MessageToMessageDecoder<?>) ctx.pipeline().get(DECOMPRESS);
        try {
            return (ByteBuf) callDecodeMessage(decompressor, ctx.pipeline().context(DECOMPRESS), bytebuf).get(0);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static ByteBuf compress(ChannelHandlerContext ctx, ByteBuf bytebuf) {
        final ChannelHandler compressor = ctx.pipeline().get(COMPRESS);
        try {
            if (compressor instanceof MessageToMessageEncoder<?> encoder) {
                return (ByteBuf) callEncodeMessage(encoder, ctx.pipeline().context(COMPRESS), bytebuf).get(0);
            } else {
                return callEncodeByteBuf((MessageToByteEncoder<?>) compressor, ctx.pipeline().context(COMPRESS), bytebuf);
            }
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}