package cn.pug.client.component.inboundHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class Client2DesInboundHandler extends ChannelInboundHandlerAdapter {

    private final Channel dstChannelFuture;

    public Client2DesInboundHandler(Channel dstChannelFuture) {
        this.dstChannelFuture = dstChannelFuture;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        dstChannelFuture.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//        log.trace("客户端与代理服务器的连接已经断开，即将断开代理服务器和目标服务器的连接");
        dstChannelFuture.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        log.error("Client2DestInboundHandler exception", cause);
    }
}
