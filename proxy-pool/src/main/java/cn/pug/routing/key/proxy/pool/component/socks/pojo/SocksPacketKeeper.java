package cn.pug.routing.key.proxy.pool.component.socks.pojo;

import cn.pug.common.protocol.parser.RoutingParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import lombok.ToString;


@ToString
public class SocksPacketKeeper {
    public RoutingParser.RoutingPacket routingPacket;
    public ChannelHandlerContext toClientCtx;
    public Socks5AddressType socks5AddressType;

    public SocksPacketKeeper(RoutingParser.RoutingPacket routingPacket, ChannelHandlerContext toClientCtx, Socks5AddressType socks5AddressType) {
        this.routingPacket=routingPacket;
        this.toClientCtx = toClientCtx;
        this.socks5AddressType = socks5AddressType;
    }

    public String getKey(){
        return this.routingPacket.getKey();
    }

    public static String getKey(RoutingParser.RoutingPacket routingPacket){
        return routingPacket.getKey();
    }
}