package cn.pug.routing.key.proxy.core.protocol.decoder.parser;

import cn.pug.routing.key.proxy.core.protocol.RoutingKeyProtocol;
import cn.pug.routing.key.proxy.core.protocol.decoder.Parser;
import io.netty.buffer.ByteBuf;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * 注册响应报文解析器
 * <p>
 * 作用：用作注册响应报文解析
 * <p>
 * 作用：用作注册响应报文解析
 * <p>
 * 报文格式：
 * <pre>
 * +--------+----------+
 * |  type  |   port   |
 * +--------+----------+
 * |  1B    |    4B    |
 * +--------+----------+
 * </pre>
 *
 * type: 响应类型，1字节
 *       - 0x00: 注册失败
 *       - 0x01: 注册成功
 *
 * port: 端口号，4字节
 *       - 当type为0x00时，该字段无意义
 *       - 当type为0x01时，表示分配给客户端的SOCKS代理端口号
 * @author pug
 * @since 1.0.0
 */
public class RegisterResponseParser implements Parser {

    public static Set<RoutingKeyProtocol.State> supportState = new HashSet<>();
    @Override
    public Set<RoutingKeyProtocol.State> support() {
        return supportState;
    }

    @Override
    public boolean isSupport(RoutingKeyProtocol.State state) {
        return support().contains(state);
    }
    @Override
    public void addSupport(RoutingKeyProtocol.State state) {
        supportState.add(state);
    }
    @Override
    public Object parser(ByteBuf in) {
        //     【1 成功】【2 port】
        if (in.readableBytes()<1){
            return null;
        }
        byte type = in.readByte();
        if (type==RegisterPacket.failVal){
            return new RegisterPacket(0,false);
        }
        // 读取port长度
        if (in.readableBytes()<4){
            return null;
        }
        int socksPort = in.readInt();
        return new RegisterPacket(socksPort,true);
    }
    @ToString
    public static class RegisterPacket{
        public static byte failVal=0x00;
        public static byte successVal=0x01;
        public boolean success;
        public int socksPort;
        public RegisterPacket(int socksPort,boolean success) {
            this.socksPort = socksPort;
            this.success=success;
        }
    }
}
