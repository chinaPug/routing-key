package cn.pug.routing.key.proxy.pool.component.socks.handler;

import cn.pug.routing.key.proxy.pool.component.daemon.Daemon;
import cn.pug.routing.key.proxy.pool.component.socks.Socks5;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectionStatisticsHandler extends ChannelInboundHandlerAdapter {

    private final Daemon daemon;

    public ConnectionStatisticsHandler(Daemon daemon) {
        if (daemon == null) {
            throw new IllegalArgumentException("Daemon cannot be null");
        }
        this.daemon = daemon;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("代理单元连接建立，地址:【{}】", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            Socks5 socks5 = daemon.getProxyUnitMap().remove(ctx.channel().remoteAddress());
            if (socks5 != null) {
                socks5.shutdownGracefully();
                log.info("代理单元下线成功，地址:【{}】，关闭本地映射端口:【{}】", 
                    ctx.channel().remoteAddress(), socks5.getClientProxyPort());
            } else {
                log.warn("代理单元下线失败，地址:【{}】，未找到本地映射端口服务", 
                    ctx.channel().remoteAddress());
            }
        } catch (Exception e) {
            log.error("处理代理单元下线时发生异常，地址:【{}】", ctx.channel().remoteAddress(), e);
        } finally {
            super.channelInactive(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("连接统计处理器异常，地址:【{}】", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
