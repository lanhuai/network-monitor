package com.lanhuai.network.monitor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new MonitorServerInitializer());

            ChannelFuture channelFuture = b.bind(port).sync();
            logger.info("Binding to port {}", port);
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
