package com.chenh.nio_reactor.multithread;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author chenh
 * @create 2021-06-29 11:18
 */
class Handler implements Runnable {

    private final static int DEFAULT_SIZE = 1024;
    private final SocketChannel socketChannel;
    private final SelectionKey seletionKey;
    private static final int READING = 0;
    private static final int SENDING = 1;
    private int state = READING;
    ByteBuffer inputBuffer = ByteBuffer.allocate(DEFAULT_SIZE);
    ByteBuffer outputBuffer = ByteBuffer.allocate(DEFAULT_SIZE);

    private Selector selector;
    private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final int PROCESSING = 3;

    public Handler(Selector selector, SocketChannel channel) throws IOException {
        this.selector = selector;
        this.socketChannel = channel;
        socketChannel.configureBlocking(false);
        this.seletionKey = socketChannel.register(selector, 0);
        seletionKey.attach(this);
        seletionKey.interestOps(SelectionKey.OP_READ);
        selector.wakeup();
    }

    @Override
    public void run() {
        if (state == READING) {
            read();
        } else if (state == SENDING) {
            write();
        }
    }

    class Sender implements Runnable {
        @Override
        public void run() {
            try {
                socketChannel.write(outputBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (outIsComplete()) {
                seletionKey.cancel();
            }
        }
    }

    private void write() {
        try {
            socketChannel.write(outputBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (outIsComplete()) {
            seletionKey.cancel();
        }
    }

    private void read() {
        try {
            socketChannel.read(inputBuffer);
            if (inputIsComplete()) {
                process();
                executorService.execute(new Processer());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean inputIsComplete() {
        return true;
    }

    public boolean outIsComplete() {
        return true;
    }

    public void process() {
    }

    synchronized void processAndHandOff() {
        process();
        state = SENDING; // or rebind attachment
        seletionKey.interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    class Processer implements Runnable {
        public void run() {
            processAndHandOff();
        }
    }
}
