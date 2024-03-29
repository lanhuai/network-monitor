package com.lanhuai.network.monitor;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.nio.charset.Charset;

/**
 * @author <a href="mailto:lanhuai@gmail.com">Ning Yubin</a>
 */
public class MonitorClientInitializer extends ChannelInitializer<SocketChannel> {
    private static final StringDecoder DECODER = new StringDecoder(Charset.forName("UTF-8"));
    private static final StringEncoder ENCODER = new StringEncoder(Charset.forName("UTF-8"));

    private final String host;
    private final int port;

    public MonitorClientInitializer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("idleStateHandler", new IdleStateHandler(MonitorClientHandler.READ_TIMEOUT, MonitorClientHandler.WRITE_TIMEOUT, 0));
        pipeline.addLast("framer", new LineBasedFrameDecoder(1024, true, true));
        pipeline.addLast("decoder", DECODER);
        pipeline.addLast("encoder", ENCODER);

        pipeline.addLast("monitorClientHandler", new MonitorClientHandler(host, port));
    }
}
