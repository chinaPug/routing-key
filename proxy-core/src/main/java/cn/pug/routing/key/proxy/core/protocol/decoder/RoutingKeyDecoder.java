package cn.pug.routing.key.proxy.core.protocol.decoder;

import cn.pug.routing.key.proxy.core.protocol.RoutingKeyProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 路由键协议解码器
 * <p>
 * 该解码器用于解析符合路由键协议格式的数据包。协议格式如下：
 * <p>
 * - 第1个字节：魔数（Magic Number），用于标识协议类型
 * <p>
 * - 第2个字节：状态类型，表示报文的具体类型
 * <p>
 * - 后续字节：根据状态类型解析的具体数据内容
 * </p>
 * <p>
 * 解码过程：
 * <p>
 * 1. 检查可读字节数是否足够（至少2个字节）
 * <p>
 * 2. 验证魔数是否正确
 * <p>
 * 3. 根据状态类型使用相应的解析器解析数据
 * <p>
 * 4. 如果解析成功，将解析结果添加到输出列表中
 * </p>
 *
 * @author pug
 * @since 1.0.0
 */

@Slf4j
public class RoutingKeyDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        log.debug("开始解析报文，可读字节数: {}", in.readableBytes());
        
        // 报文类型中，最少的报文类型需要2个字节，小于两个字节无法解析
        if (in.readableBytes() < 2) {
            log.debug("报文长度不足2个字节，无法解析");
            return;
        }
        
        // 记录读取下标，便于还原
        in.markReaderIndex();
        
        try {
            byte magic = in.readByte();
            if (magic != RoutingKeyProtocol.MAGIC_NUMBER) {
                in.resetReaderIndex();
                log.error("无效报文，magic number错误: 0x{}, 期望: 0x{}", 
                    Integer.toHexString(magic & 0xFF), 
                    Integer.toHexString(RoutingKeyProtocol.MAGIC_NUMBER));
                ctx.close(); // 关闭连接而不是抛出异常
                return;
            }
            
            byte type = in.readByte();
            int typeIndex = type & 0xFF;
            
            // 验证消息类型是否有效
            if (typeIndex >= RoutingKeyProtocol.State.values().length) {
                in.resetReaderIndex();
                log.error("无效的消息类型: {}", typeIndex);
                ctx.close();
                return;
            }
            
            RoutingKeyProtocol.State state = RoutingKeyProtocol.State.values()[typeIndex];
            
            // 使用相应的解码器解析数据
            Object parsedObject = state.parser.parser(in);
            
            // 字节数不够，等待更多数据
            if (parsedObject == null) {
                in.resetReaderIndex();
                log.debug("字节数不足，等待更多数据");
                return;
            }
            
            // 添加到输出列表中
            out.add(parsedObject);
            log.debug("解析成功，报文类型: {}, 内容: {}", state, parsedObject);
            
        } catch (Exception e) {
            in.resetReaderIndex();
            log.error("解析报文时发生异常", e);
            ctx.close();
        }
    }
}
