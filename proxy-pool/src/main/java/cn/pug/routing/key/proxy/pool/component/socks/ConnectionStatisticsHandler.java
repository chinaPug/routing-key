package cn.pug.routing.key.proxy.pool.component.socks;

import cn.pug.routing.key.proxy.pool.component.daemon.Daemon;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

@Slf4j
public class ConnectionStatisticsHandler extends ChannelInboundHandlerAdapter {

    private final Daemon daemon;

    public ConnectionStatisticsHandler(Daemon daemon) {
        this.daemon = daemon;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Socks5 socks5= daemon.getProxyUnitMap().remove(ctx.channel().remoteAddress());
        if (socks5 != null) {
            socks5.shutdownGracefully();
            log.info("unit服务下线成功，地址:【{}】，关闭本地映射端口:【{}】", ctx.channel().remoteAddress(),socks5.getClientProxyPort());
        }else {
            log.warn("unit服务下线失败，地址:【{}】，未找到本地映射端口服务", ctx.channel().remoteAddress());
        }

    }
}
