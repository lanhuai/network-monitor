package com.lanhuai.network.monitor;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.nio.charset.Charset;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:lanhuai@gmail.com">Ning Yubin</a>
 */
public class MonitorServerInitializer extends ChannelInitializer<SocketChannel> {
    private static final StringDecoder DECODER = new StringDecoder(Charset.forName("UTF-8"));
    private static final StringEncoder ENCODER = new StringEncoder(Charset.forName("UTF-8"));
    private static final DefaultEventExecutorGroup eventExecutorGroup = new DefaultEventExecutorGroup(//
            8, //
            new ThreadFactory() {

                private final AtomicInteger threadIndex = new AtomicInteger(0);
                private static final int threadTotal = 8;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyServerHandler-%d-%d", threadTotal,
                            threadIndex.incrementAndGet()));
                }
            });

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(eventExecutorGroup,
                new IdleStateHandler(MonitorClientHandler.READ_TIMEOUT, 0, 0),
                new LineBasedFrameDecoder(1024, true, true),
                DECODER,
                ENCODER,
                new MonitorServerHandler()
                );
    }
}
