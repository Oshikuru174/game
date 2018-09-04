package org.fandyrev.game.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast("decoder", new HttpRequestDecoder());
        p.addLast("encoder", new HttpResponseEncoder());
        p.addLast("aggregator", new HttpObjectAggregator(Short.MAX_VALUE));
        p.addLast("handler", new GameHandler());
    }
}