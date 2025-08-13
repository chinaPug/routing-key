package cn.pug.routing.key.proxy.pool.component.daemon.handler;

import cn.pug.routing.key.proxy.core.protocol.decoder.parser.RegisterRequestParser;
import cn.pug.routing.key.proxy.core.util.NetUtil;
import cn.pug.routing.key.proxy.pool.component.daemon.Daemon;
import cn.pug.routing.key.proxy.pool.component.socks.Socks5;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UnitRegisterInbounderHandler extends SimpleChannelInboundHandler<RegisterRequestParser.Null> {
    private final Daemon daemon;
    public UnitRegisterInbounderHandler(Daemon daemon) {
        this.daemon = daemon;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext toUnitDaemon, RegisterRequestParser.Null msg) throws Exception {
        log.info("unit服务注册成功，地址:【{}】",toUnitDaemon.channel().remoteAddress());
        Socks5 socks5 = new Socks5(toUnitDaemon, NetUtil.getAvailablePort(),daemon);
        // 3 启动代理服务
        socks5.start();
    }
}
