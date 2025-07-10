package cn.pug.routing.key.proxy.pool.component.daemon;

import cn.pug.routing.key.proxy.pool.component.socks.Socks5;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;


@Slf4j
class TcpProxyRegistryHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext toClientDaemon, String registerPacket) throws Exception {
        log.info("收到代理注册请求：" + registerPacket);
        Socks5 socks5 = new Socks5(toClientDaemon.channel());
        // 3 启动代理服务
        socks5.start();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("daemon服务异常",cause);
    }
}