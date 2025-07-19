package cn.pug.common.protocol;


import cn.pug.common.protocol.parser.NullParser;
import cn.pug.common.protocol.parser.RegisterParser;
import cn.pug.common.protocol.parser.Parser;
import cn.pug.common.protocol.parser.RoutingParser;

public class RoutingKeyProtocol {
    // 段分隔符
    public static final String SEGMENT_SPLIT = " ";
    // 魔数
    public static final int MAGIC_NUMBER = 0xFF&0x44;


    public enum State{
        REGISTER_REQUEST(0x00,"注册请求",new NullParser()),
        REGISTER_RESPONSE(0x01,"注册响应",new RegisterParser()),
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
