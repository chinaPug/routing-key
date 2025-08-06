package cn.pug.routing.key.proxy.pool.component.socks;

import cn.pug.common.handler.P2PInboundHandler;
import cn.pug.common.protocol.RoutingKeyDecoder;
import cn.pug.common.protocol.parser.RoutingParser;
import cn.pug.routing.key.proxy.pool.component.socks.pojo.SocksPacketKeeper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoutingResponseInboundHandler extends SimpleChannelInboundHandler<RoutingParser.RoutingPacket> {
    private Socks5 socks5;

    public RoutingResponseInboundHandler(Socks5 socks5) {
        this.socks5 = socks5;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext toUnitCtx, RoutingParser.RoutingPacket routingPacket) {
        SocksPacketKeeper socksPacketKeeper = socks5.socksPacketKeeperConcurrentHashMap.remove(SocksPacketKeeper.getKey(routingPacket));
        socks5.socksPacketKeeperConcurrentHashMapPrinter();
        if (routingPacket.success) {
            log.info("unit 连接目标地址成功");
            // 移除routing key协议处理器
            toUnitCtx.pipeline().remove(RoutingKeyDecoder.class);
            toUnitCtx.pipeline().remove(this);
            // 添加转发处理器
            toUnitCtx.pipeline()
                    .addLast(new P2PInboundHandler(socksPacketKeeper.toClientCtx.channel()));
            socksPacketKeeper.toClientCtx.pipeline().addLast(new P2PInboundHandler(toUnitCtx.channel()));
            //转发成功 Client2ServerInboundHandler Client2ServerInboundHandler
            DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socksPacketKeeper.socks5AddressType);
            socksPacketKeeper.toClientCtx.channel().writeAndFlush(commandResponse);
//            socksPacketKeeper.toClientCtx.pipeline().remove(Socks5CommandRequestDecoder.class);
        } else {
            log.info("unit 连接目标地址失败");
            //转发失败
            DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socksPacketKeeper.socks5AddressType);
            socksPacketKeeper.toClientCtx.channel().writeAndFlush(commandResponse);
        }
    }
}
