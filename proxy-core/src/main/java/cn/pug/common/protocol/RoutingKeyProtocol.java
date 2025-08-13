package cn.pug.common.protocol;


import cn.pug.common.protocol.decoder.parser.RegisterRequestParser;
import cn.pug.common.protocol.decoder.Parser;
import cn.pug.common.protocol.decoder.parser.RegisterResponseParser;
import cn.pug.common.protocol.decoder.parser.RoutingParser;

/**
 * routing key协议
 *
 * @author pug
 * @since 1.0.0
 * 协议报文格式:
 * +---------------------------------------------------------------+
 * | 魔数(1byte) | 消息类型(1byte) | 数据体(可变长字节)                |
 * +---------------------------------------------------------------+
 * 数据体长度根据具体消息类型确定，可能为固定长度或可变长度。
 */
public class RoutingKeyProtocol {
    public static final int MAGIC_NUMBER = 0xFF&0x44;

    public enum State{
        REGISTER_REQUEST(0x00,"注册请求",new RegisterRequestParser()),
        REGISTER_RESPONSE(0x01,"注册响应",new RegisterResponseParser()),
        ROUTING_REQUEST(0x02,"转发请求",new RoutingParser()),
        ROUTING_RESPONSE(0x03,"转发成功",new RoutingParser());

        public final int type;
        public final String des;
        public final Parser parser;
        State(int type, String des, Parser parser){
            this.type = type;
            this.des = des;
            this.parser = parser;
            parser.addSupport( this);
        }
    }


}
