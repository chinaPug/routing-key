package cn.pug.routing.key.proxy.pool.component.daemon;

import cn.pug.common.protocol.parser.NullParser;
import cn.pug.common.util.NetUtil;
import cn.pug.routing.key.proxy.pool.component.socks.Socks5;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UnitRegisterInbounderHandler extends SimpleChannelInboundHandler<NullParser.Null> {
    private final Daemon daemon;
    public UnitRegisterInbounderHandler(Daemon daemon) {
        this.daemon = daemon;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext toUnitDaemon, NullParser.Null msg) throws Exception {
        log.info("unit服务注册成功，地址:【{}】",toUnitDaemon.channel().remoteAddress());
        Socks5 socks5 = new Socks5(toUnitDaemon, NetUtil.getAvailablePort(),daemon);
        // 3 启动代理服务
        socks5.start();
    }
}
