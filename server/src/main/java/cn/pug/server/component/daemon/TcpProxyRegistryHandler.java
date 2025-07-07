package cn.pug.server.component.daemon;

import cn.pug.common.utils.NetUtil;
import cn.pug.server.component.socks.Socks5;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.IntStream;

@Slf4j
class TcpProxyRegistryHandler extends SimpleChannelInboundHandler<String> {
    private int portStart;
    private int portEnd;
    public TcpProxyRegistryHandler(int portStart,int portEnd) {
        this.portStart = portStart;
        this.portEnd = portEnd;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String registerPacket) throws Exception {
        log.info("收到代理注册请求：" + registerPacket);
        // 1 找到一个可用的端口号
        int availablePort = IntStream.rangeClosed(this.portStart, this.portEnd)
                .filter(NetUtil::isPortCanUse)
                .findFirst().orElse(-1);
        if (availablePort != -1){
            // 2 进行注册，注册逻辑在socks5.start()中
            Socks5 socks5 = new Socks5(availablePort);
            // 3 启动代理服务
            socks5.start(ctx);
        }else {
            log.error("无可用端口，地址【{}】注册请求被丢弃。",ctx.channel().remoteAddress());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("代理注册服务异常",cause);
    }
}