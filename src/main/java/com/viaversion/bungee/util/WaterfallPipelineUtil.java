package com.viaversion.bungee.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class WaterfallPipelineUtil {
    public static final Method ENCODE_METHOD;
    public static boolean IS_WATERFALL;

    static {
        try {
            Class.forName("io.github.waterfallmc.waterfall.conf.WaterfallConfiguration");
            IS_WATERFALL = true;
        } catch (ClassNotFoundException e) {
            IS_WATERFALL = false;
        }
        try {
            ENCODE_METHOD = MessageToMessageEncoder.class.getDeclaredMethod("encode", ChannelHandlerContext.class, Object.class, List.class);
            ENCODE_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Object> callEncode(MessageToMessageEncoder encoder, ChannelHandlerContext ctx, ByteBuf input) throws InvocationTargetException {
        List<Object> output = new ArrayList<>();
        try {
            ENCODE_METHOD.invoke(encoder, ctx, input, output);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return output;
    }

    public static ByteBuf compress(ChannelHandlerContext ctx, ByteBuf bytebuf) {
        try {
            return (ByteBuf) callEncode((MessageToMessageEncoder) ctx.pipeline().get("compress"), ctx.pipeline().context("compress"), bytebuf).get(0);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return ctx.alloc().buffer();
        }
    }
}

