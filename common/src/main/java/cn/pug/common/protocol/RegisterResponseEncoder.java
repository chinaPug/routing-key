package cn.pug.common.protocol;

import cn.pug.common.protocol.parser.RegisterParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import static cn.pug.common.protocol.RoutingKeyProtocol.State.REGISTER_RESPONSE;

@Slf4j
public class RegisterResponseEncoder extends MessageToByteEncoder<RegisterParser.RegisterPacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RegisterParser.RegisterPacket registerPacket, ByteBuf out) throws Exception {
        log.info("编码注册响应信息:【{}】",registerPacket);
        // 写魔数
        out.writeByte(RoutingKeyProtocol.MAGIC_NUMBER);
        // 写类型
        out.writeByte(REGISTER_RESPONSE.type);
        // 写成功
        out.writeBoolean(registerPacket.success);
        // 写socks5端口
        out.writeInt(registerPacket.socksPort);
    }
}
