package cn.pug.common.protocol.parser;

import cn.pug.common.protocol.RoutingKeyProtocol;
import io.netty.buffer.ByteBuf;

import java.util.HashSet;
import java.util.Set;

public class NullParser implements Parser {
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
