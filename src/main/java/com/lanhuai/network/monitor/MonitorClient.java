package com.lanhuai.network.monitor;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * @author <a href="mailto:lanhuai@gmail.com">Ning Yubin</a>
 */
public final class MonitorClient {
    private static final Logger logger = LoggerFactory.getLogger(MonitorClient.class);
    private static final Pattern HOST_PORT_DELIMITER_PATTERN = Pattern.compile(":");

    private static volatile boolean running = true;

    private static final String[] SERVERS = ConfigUtils.getValues("servers");

    private static final ChannelFutureListener channelFutureListener = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                logger.info("Connection established for channel: {}", future.channel());
            } else {
                Throwable cause = future.cause();
                if (cause != null) {
                    logger.error("Failed to create connection: " + cause.getMessage(), cause);
                }
            }
        }
    };

    private MonitorClient() {
    }

    public static ChannelFuture connect(Bootstrap b, String host, int port) {
        return b.handler(new MonitorClientInitializer(host, port))
                .connect(host, port).addListener(channelFutureListener);
    }

    static Bootstrap configureBootstrap(Bootstrap b, EventLoopGroup g) {
        b.group(g)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true);
        return b;
    }

    public static void main(String[] args) throws Exception {
        if (SERVERS.length == 0) {
            logger.info("no server configuration in config.properties");
            return;
        }
        addShutdownHook();

        EventLoopGroup workGroup = new NioEventLoopGroup();

        Bootstrap b = new Bootstrap();
        b = configureBootstrap(b, workGroup);
        try {
            for (String server : SERVERS) {
                String[] serverHostPort = HOST_PORT_DELIMITER_PATTERN.split(server);

                b = b.clone().handler(new MonitorClientInitializer(serverHostPort[0]
                        , Integer.parseInt(serverHostPort[1])));

                ChannelFuture channelFuture = b.connect(serverHostPort[0]
                        , Integer.parseInt(serverHostPort[1]));

                channelFuture.addListener(channelFutureListener);

                logger.info("Initialize connection with server {}", server);
                // Wait until the connection is closed.
                // channelFuture.channel().closeFuture().sync();
            }
            logger.info("MonitorClient Start Success!");

            synchronized (MonitorClient.class) {
                while (running) {
                    try {
                        logger.info("Waiting on shutdown signal, ctrl-c or kill -15 pid.");
                        MonitorClient.class.wait();
                    } catch (Throwable e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }

        } finally {
            logger.info("Shutdown Gracefully!");
            workGroup.shutdownGracefully();
        }
    }

    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (MonitorClient.class) {
                    logger.info("Notify main thread to stop!");
                    running = false;
                    MonitorClient.class.notify();
                }
            }
        }));
    }

}
