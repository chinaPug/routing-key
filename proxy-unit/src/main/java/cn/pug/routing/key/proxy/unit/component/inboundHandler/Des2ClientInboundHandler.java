package cn.pug.routing.key.proxy.unit.component.inboundHandler;


import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Des2ClientInboundHandler extends ChannelInboundHandlerAdapter {

    private final Channel toServerChannel;

    public Des2ClientInboundHandler(Channel toServerChannel) {
        this.toServerChannel = toServerChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.trace("开始写回客户端");
        toServerChannel.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.trace("代理服务器和目标服务器的连接已经断开，即将断开客户端和代理服务器的连接");
        toServerChannel.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Dest2ClientInboundHandler exception", cause);
    }
}
