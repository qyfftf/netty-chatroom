package com.fftf.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.*;

public class ChatServerHandler extends SimpleChannelInboundHandler<String> {
    private static final Map<ChannelHandlerContext, String> clients = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected: " + ctx.channel().remoteAddress());
        ctx.writeAndFlush("请输入你的用户名：\n");

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected: " + clients.get(ctx));
        clients.remove(ctx);
        broadcastMessage("User left: " + clients.get(ctx));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (!clients.containsKey(ctx)) {
            if (!msg.isEmpty()) {
                if (clients.containsValue(msg)) {
                    ctx.writeAndFlush("用户名已经存在，请重新输入用户名：\n");
                    return;
                }
                clients.put(ctx, msg.trim());
                broadcastMessage("用户加入聊天室: " + msg);
            } else {
                ctx.writeAndFlush("无效的用户名，请重新输入用户名：\n");
                return;
            }
        } else {
            System.out.println("接收到消息 " + clients.get(ctx) + ": " + msg);
            if (msg.startsWith("/msg ")) {
                handlePrivateMessage(ctx, msg.substring(5));
            } else {
                broadcastMessage(clients.get(ctx) + ": " + msg);
            }
        }
    }

    private void broadcastMessage(String msg) {
        synchronized (clients) {
            for (ChannelHandlerContext client : clients.keySet()) {
                client.writeAndFlush(msg + "\n");
            }
        }
    }

    private void handlePrivateMessage(ChannelHandlerContext senderCtx, String msg) {
        String[] parts = msg.split(" ", 2);
        if (parts.length != 2) {
            senderCtx.writeAndFlush("无效的消息格式 使用 /msg <username> <message>\n");
            return;
        }

        String recipientUsername = parts[0];
        String message = parts[1];

        for (Map.Entry<ChannelHandlerContext, String> entry : clients.entrySet()) {
            if (entry.getValue().equals(recipientUsername)) {
                entry.getKey().writeAndFlush("[私发消息 来自于 " + clients.get(senderCtx) + "] " + message + "\n");
                return;
            }
        }

        senderCtx.writeAndFlush("用户未找到: " + recipientUsername + "\n");
    }
}
