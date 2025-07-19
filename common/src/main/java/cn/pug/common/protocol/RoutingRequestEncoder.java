package cn.pug.common.protocol;

import cn.pug.common.protocol.parser.RoutingParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import static cn.pug.common.protocol.RoutingKeyProtocol.State.ROUTING_REQUEST;

@Slf4j
public class RoutingRequestEncoder extends MessageToByteEncoder<RoutingParser.RoutingPacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RoutingParser.RoutingPacket routingPacket, ByteBuf out) throws Exception {
        log.info("转发请求编码开始【{}】", routingPacket);
        // 写魔数
        out.writeByte(RoutingKeyProtocol.MAGIC_NUMBER);
        // 写类型
        out.writeByte(ROUTING_REQUEST.type);
        // 写可变长
        int ipVarLength = routingPacket.desIp.length();
        out.writeInt(ipVarLength);
        // 写ip
        out.writeBytes(routingPacket.desIp.getBytes());
        // 写port
        out.writeInt(routingPacket.desPort);
        // 写成功失败
        if (routingPacket.success){
            out.writeByte(RoutingParser.RoutingPacket.successVal);
        }else {
            out.writeByte(RoutingParser.RoutingPacket.failVal);
        }
        // 写identifyVal
        out.writeBytes(routingPacket.identifyVal.getBytes());
    }
}
