package com.chenh.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * @author chenh
 * @date 2021年03月13日
 */
public class ServerHandler implements Runnable {

    private Selector selector = null;
    private ServerSocketChannel serverChannel = null;
    private boolean stop;

    /**
     * NIO服务端代码（新建连接）
     *
     * @param port
     */
    public ServerHandler(int port) {
        try {

            //获取一个ServerSocket通道
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(port), 1024);
            //获取通道管理器
            selector = Selector.open();
            //将通道管理器与通道绑定，并为该通道注册SelectionKey.OP_ACCEPT事件
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("服务器监听" + port);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void stop() {
        this.stop = true;
    }

    /**
     * NIO服务端代码（监听）
     */
    public void run() {
        while (!stop) {
            try {

                //当有注册的事件到达时，方法返回，否则阻塞。
                selector.select(1000);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                SelectionKey key = null;
                while (it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        handleInput(key);
                    } catch (IOException e) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null)
                                key.channel().close();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理事件
     *
     * @param key
     * @throws IOException
     */
    private void handleInput(SelectionKey key) throws IOException {
        if (key.isValid()) {
            if (key.isAcceptable()) {

                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                //在与客户端连接成功后，为客户端通道注册SelectionKey.OP_READ事件。
                sc.register(selector, SelectionKey.OP_READ);

            }
            if (key.isReadable()) { //有可读数据事件
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer readBuff = ByteBuffer.allocate(1024);
                //非阻塞的
                int read = sc.read(readBuff);
                if (read > 0) {
                    readBuff.flip();
                    byte[] bytes = new byte[readBuff.remaining()];
                    readBuff.get(bytes);
                    String body = new String(bytes, "utf-8");
                    System.out.println("服务收到消息：" + body);
                    String currentTime = new Date(System.currentTimeMillis()).toString();
                    doWrite(sc, currentTime);
                } else if (read < 0) {
                    key.cancel();
                    sc.close();
                } else {
                }
            }
        }
    }

    /**
     * 异步发送应答消息
     *
     * @param sc
     * @param content
     * @throws IOException
     */
    private void doWrite(SocketChannel sc, String content) throws IOException {
        if (content != null && content.trim().length() > 0) {
            byte[] bytes = content.getBytes();
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
            byteBuffer.put(bytes);
            byteBuffer.flip();
            sc.write(byteBuffer);
        }
    }
}
