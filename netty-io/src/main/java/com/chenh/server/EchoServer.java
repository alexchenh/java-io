package com.chenh.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * @author chenh
 * @date 2021年03月13日
 */
public class EchoServer {
    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage:" + EchoServer.class.getSimpleName() + "<port>");
        }
        int port = Integer.parseInt(args[0]);
        new EchoServer(port).start();
    }

    public void start() throws Exception {
        final EchoServerHandler echoServerHandler = new EchoServerHandler();

        //创建EventLoopGroup
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            //创建ServerBootstrap
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class) //指定所使用的NIO传输Channel
                    .localAddress(new InetSocketAddress(port)) //使用指定的端口设置套接字地址
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(echoServerHandler); //EchoServerHandler被标注为@Shareable，所以我们可以总是使用同样的实例
                        }
                    }); //添加一个EchoServerHandler到子Channel的ChannelPipeline

            //异步地绑定服务器；调用 sync()方法阻塞，等待直到绑定完成
            ChannelFuture f = b.bind().sync();

            //获取 Channel的CloseFuture，并且阻塞当前线程直到它完成
            f.channel().closeFuture().sync();
        } finally {
            //关闭 EventLoopGroup，释放所有的资源
            group.shutdownGracefully().sync();
        }

    }
}
