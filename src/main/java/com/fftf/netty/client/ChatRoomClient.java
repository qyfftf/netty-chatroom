package com.fftf.netty.client;

import com.fftf.netty.handler.ChatClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ChatRoomClient {
    private final String host;
    private final int port;
    private  Channel channel;

    public ChatRoomClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public Channel getChannel() {
        return channel;
    }

    public void run() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new ChatClientHandler(ChatRoomClient.this));
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();

            // 启动一个线程用于接收用户的输入
            Thread userInputThread = new Thread(() -> {
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    try {
                        String msg = in.readLine();
                        if (msg == null || "exit".equalsIgnoreCase(msg)) {
                            break;
                        }
                        channel.writeAndFlush(msg + "\n");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                channel.close();
            });
            userInputThread.start();

            // 等待直到客户端连接关闭
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 8080;
        if (args.length == 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }
        new ChatRoomClient(host, port).run();
    }
}
