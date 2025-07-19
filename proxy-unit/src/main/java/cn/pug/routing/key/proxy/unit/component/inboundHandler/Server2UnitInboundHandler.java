package cn.pug.routing.key.proxy.unit.component.inboundHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Server2UnitInboundHandler extends ChannelInboundHandlerAdapter {

    private final Channel toDesChannel;

    public Server2UnitInboundHandler(Channel toDesChannel) {
        this.toDesChannel = toDesChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        toDesChannel.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.trace("客户端与代理服务器的连接已经断开，即将断开代理服务器和目标服务器的连接");
        toDesChannel.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Client2DestInboundHandler exception", cause);
    }
}
