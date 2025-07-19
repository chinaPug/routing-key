package cn.pug.routing.key.proxy.pool.component.socks.inboundHandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Browser2ServerInboundHandler extends ChannelInboundHandlerAdapter {

    private final ChannelHandlerContext toClientCtx;

    public Browser2ServerInboundHandler(ChannelHandlerContext toClientCtx) {
        this.toClientCtx = toClientCtx;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        toClientCtx.channel().writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.trace("代理服务器和目标服务器的连接已经断开，即将断开客户端和代理服务器的连接");
        toClientCtx.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Dest2ClientInboundHandler exception", cause);
    }
}
