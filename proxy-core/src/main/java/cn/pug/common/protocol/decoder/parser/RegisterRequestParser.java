package cn.pug.common.protocol.decoder.parser;

import cn.pug.common.protocol.RoutingKeyProtocol;
import cn.pug.common.protocol.decoder.Parser;
import io.netty.buffer.ByteBuf;

import java.util.HashSet;
import java.util.Set;

/**
 * 空报文解析器，用于不需解析的报文类型
 * <p>
 *     作用： 用于注册请求报文的解析
 * @author pug
 * @since 1.0.0
 */
public class RegisterRequestParser implements Parser {
    private static Set<RoutingKeyProtocol.State> supportState = new HashSet<>();

    static Null parserObject=new Null();
    @Override
    public boolean isSupport(RoutingKeyProtocol.State state) {
        return support().contains(state);
    }

    @Override
    public void addSupport(RoutingKeyProtocol.State state) {
        supportState.add(state);
    }

    @Override
    public Set<RoutingKeyProtocol.State> support() {
        return supportState;
    }

    @Override
    public Object parser(ByteBuf in) {
        return parserObject;
    }

    public static class Null{

    }
}
