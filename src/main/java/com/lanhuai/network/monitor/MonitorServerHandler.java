package com.lanhuai.network.monitor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author <a href="mailto:lanhuai@gmail.com">Ning Yubin</a>
 */
public class MonitorServerHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger logger = LoggerFactory.getLogger(MonitorServerHandler.class);

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Connected from {} established", ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
//        super.channelRead(ctx, msg);
        logger.debug("Read message from {} : {}", ctx.channel().remoteAddress(), msg);

        String respoMsg = "alive";
        ctx.writeAndFlush(respoMsg + LINE_SEPARATOR);
        logger.debug("Send message to {} : {}", ctx.channel().remoteAddress(), respoMsg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                logger.error("remote client {} not active", ctx.channel().remoteAddress());
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                logger.info("server write idle");
                ctx.writeAndFlush("test" + LINE_SEPARATOR);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel {} disconnected", ctx.channel());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Unregist channel {}", ctx.channel());
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Regist channel {}", ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(String.format("%s on channel %s", cause.getMessage(), ctx.channel()), cause);
        if (cause instanceof IOException) {
            ctx.close();
        }
    }
}
