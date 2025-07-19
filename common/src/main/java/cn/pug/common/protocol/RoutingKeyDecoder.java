package cn.pug.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RoutingKeyDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        log.info("开始解析报文");
        // 报文类型中，最少的报文类型需要2个字节，小于两个字节无法解析
        if (in.readableBytes()<2){
            log.info("报文长度不足2个字节，无法解析");
            return;
        }
        // 记录读取下标，便于还原
        in.markReaderIndex();
        byte magic = in.readByte();
        if (magic!= RoutingKeyProtocol.MAGIC_NUMBER){
            in.resetReaderIndex();
            log.error("无效报文，magic number错误:【{}】",magic);
            throw new RuntimeException("无效报文，magic number错误");
        }
        byte type = in.readByte();
        // 这里恰好用了我们的业务定义顺序与枚举值顺序ord一致
        RoutingKeyProtocol.State state = RoutingKeyProtocol.State.values()[0xFF&type];
        // 这里需要根据请求类型来使用相应的解码器
        Object parsedObject = state.parser.parser(in);
        // 字节数不够
        if (parsedObject==null){
            in.resetReaderIndex();
            return;
        }
        // 添加到out中
        out.add(parsedObject);
        log.info("解析成功，报文类型：{}",state);
        log.info("解析成功，报文内容：{}",parsedObject);
    }
}
