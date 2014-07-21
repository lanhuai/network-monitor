package com.lanhuai.network.monitor;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:lanhuai@gmail.com">Ning Yubin</a>
 */
public class MonitorClientHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger logger = LoggerFactory.getLogger(MonitorClientHandler.class);

    public static final int RECONNECT_DELAY = Integer.parseInt(System.getProperty("reconnect-delay", "5"));
    public static final int WRITE_TIMEOUT = Integer.parseInt(System.getProperty("write-timeout", "5"));
    public static final int READ_TIMEOUT = Integer.parseInt(System.getProperty("read-timeout", "10"));

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final String host;
    private final int port;

    public MonitorClientHandler(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Connected to {}", ctx.channel().remoteAddress());
        String requestMsg = "Hello, this is " + ctx.channel().localAddress();
        ctx.writeAndFlush(requestMsg + LINE_SEPARATOR);
        logger.info("Send message to {} : {}", ctx.channel().remoteAddress(), requestMsg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        logger.debug("Read message from {} : {}", ctx.channel().remoteAddress(), msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                logger.error("server {} not active", ctx.channel().remoteAddress());
            } else if (e.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush("heartbeat" + LINE_SEPARATOR);
                logger.debug("send heartbeat to {}", ctx.channel().remoteAddress());
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel {} disconnected", ctx.channel());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Sleeping for: {}s", RECONNECT_DELAY);

        final EventLoop loop = ctx.channel().eventLoop();
        loop.schedule(new Runnable() {
            @Override
            public void run() {
                logger.info("Reconnecting to {}:{}", host, port);
                MonitorClient.connect(MonitorClient.configureBootstrap(new Bootstrap(), loop), host, port);
            }
        }, RECONNECT_DELAY, TimeUnit.SECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(String.format("%s on channel %s", cause.getMessage(), ctx.channel()), cause);
        if (cause instanceof IOException) {
            ctx.close();
        }
    }
}
