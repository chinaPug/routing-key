package cn.pug.client.component.init;

import cn.pug.common.handler.ExceptionHandler;
import cn.pug.common.protocol.RoutingKeyProtocol;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class TcpHandshakeInboundHandler extends SimpleChannelInboundHandler<String> {
    private EventLoopGroup proxyGroup;
    private String ServerIp;
    public TcpHandshakeInboundHandler(EventLoopGroup proxyGroup, String ServerIp) {
        this.proxyGroup=proxyGroup;
        this.ServerIp=ServerIp;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String socks5ServerMetadata) {
        log.info("收到代理服务元信息：{}", socks5ServerMetadata);
        String[] socks5ServerMetadataArray = socks5ServerMetadata.split(RoutingKeyProtocol.SEGMENT_SPLIT);
        if (socks5ServerMetadataArray.length==2) {
            log.info("代理连接成功:【{}】",socks5ServerMetadata);
            ctx.pipeline().remove(this);
            ctx.pipeline()
                    .addLast(new TcpProxyInboundHandler(proxyGroup, ServerIp))
                    .addLast(new ExceptionHandler());
        }else {
            log.error("代理连接失败，无效报文【{}】",socks5ServerMetadata);
        }
    }
}
