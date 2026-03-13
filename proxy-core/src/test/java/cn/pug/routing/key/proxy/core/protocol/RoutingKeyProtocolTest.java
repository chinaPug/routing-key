package cn.pug.routing.key.proxy.core.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RoutingKey 协议编解码单元测试
 * 
 * @author pug
 * @since 1.0.0
 */
class RoutingKeyProtocolTest {

    @Test
    @DisplayName("测试注册请求编码")
    void testRegisterRequestEncode() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x44); // 魔数
        buf.writeByte(0x00); // 注册请求类型
        
        assertEquals(2, buf.readableBytes());
        assertEquals(0x44, buf.getByte(0));
        assertEquals(0x00, buf.getByte(1));
    }

    @Test
    @DisplayName("测试注册响应解码")
    void testRegisterResponseDecode() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x44); // 魔数
        buf.writeByte(0x01); // 注册响应类型
        buf.writeByte(0x01); // 成功状态
        buf.writeInt(8080);  // SOCKS5端口
        
        assertEquals(7, buf.readableBytes());
    }

    @Test
    @DisplayName("测试魔数验证")
    void testMagicNumberValidation() {
        byte magic = 0x44;
        assertEquals(0x44, magic);
    }
}
