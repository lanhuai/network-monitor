package com.lanhuai.network.monitor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:lanhuai@gmail.com">Ning Yubin</a>
 */
public class MonitorServer {
    private static final Logger logger = LoggerFactory.getLogger(MonitorServer.class);
    private final int port;

    private static volatile boolean running = true;

    public MonitorServer(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(2, new ThreadFactory() {
            private final AtomicInteger threadIndex = new AtomicInteger(0);
            private static final int threadTotal = 2;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r,
                        String.format("NettyBossSelector-%d-%d", threadTotal, threadIndex.incrementAndGet()));
            }
        });
        EventLoopGroup workerGroup = new NioEventLoopGroup(4, new ThreadFactory() {
            private final AtomicInteger threadIndex = new AtomicInteger(0);
            private static final int threadTotal = 4;


            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("NettyServerWorker-%d-%d", threadTotal,
                        threadIndex.incrementAndGet()));
            }
        });
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new MonitorServerInitializer());

            ChannelFuture channelFuture = b.bind(port).sync();
            logger.info("Binding to port {}", port);

            runClient();

            channelFuture.channel().closeFuture().sync();
            logger.info("Stopping finished!");

            synchronized (MonitorServer.class) {
                while (running) {
                    try {
                        logger.info("Waiting on shutdown signal, ctrl-c or kill -15 pid.");
                        MonitorServer.class.wait();
                    } catch (Throwable e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }

        } finally {
            logger.info("Shutdown Gracefully!");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    void runClient() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MonitorClient.main(new String[]{});
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }


    public static void main(String[] args) throws Exception {
        addShutdownHook();
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }
        new MonitorServer(port).run();
    }

    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (MonitorServer.class) {
                    logger.info("Notify main thread to stop!");
                    running = false;
                    MonitorServer.class.notify();
                }
            }
        }));
    }

}
