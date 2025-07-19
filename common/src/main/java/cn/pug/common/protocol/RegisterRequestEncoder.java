package cn.pug.common.protocol;

import cn.pug.common.protocol.parser.RegisterParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import static cn.pug.common.protocol.RoutingKeyProtocol.State.REGISTER_REQUEST;
@Slf4j
public class RegisterRequestEncoder extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        log.info("编码注册请求");
        // 写魔数
        out.writeByte(RoutingKeyProtocol.MAGIC_NUMBER);
        // 写类型
        out.writeByte(REGISTER_REQUEST.type);

    }
}
