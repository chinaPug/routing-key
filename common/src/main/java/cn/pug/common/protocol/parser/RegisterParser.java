package cn.pug.common.protocol.parser;

import cn.pug.common.protocol.RoutingKeyProtocol;
import io.netty.buffer.ByteBuf;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

public class RegisterParser implements Parser {

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
