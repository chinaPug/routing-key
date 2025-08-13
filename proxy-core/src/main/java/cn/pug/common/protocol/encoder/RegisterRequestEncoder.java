package cn.pug.common.protocol.encoder;

import cn.pug.common.protocol.RoutingKeyProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import static cn.pug.common.protocol.RoutingKeyProtocol.State.REGISTER_REQUEST;

/**
 * 注册请求编码器
 *
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class RegisterRequestEncoder extends MessageToByteEncoder<Object> {
    // 注册的没有数据报，所以这里写一个空字符串，仅做占位，不会发送
    public static final String MSG = "";

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        log.info("编码注册请求");
        // 写魔数
        out.writeByte(RoutingKeyProtocol.MAGIC_NUMBER);
        // 写类型
        out.writeByte(REGISTER_REQUEST.type);

    }
}
