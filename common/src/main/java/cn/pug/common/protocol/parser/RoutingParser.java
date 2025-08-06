package cn.pug.common.protocol.parser;

import cn.pug.common.protocol.RoutingKeyProtocol;
import io.netty.buffer.ByteBuf;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RoutingParser implements Parser {
    private static final Set<RoutingKeyProtocol.State> supportState = new HashSet<>();

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
        // 小于8字节说明长度不够
        // 1var 1min_len 4port 1success 36identify_val
        if (in.readableBytes() < 1+1+4+1+36) {
            return null;
        }
        // 读取ip长度
        int ipVarLength = in.readInt();
        //
        if (in.readableBytes() < ipVarLength + 4+1+36) {
            return null;
        }
        byte[] ipBytes = new byte[(int) ipVarLength];
        in.readBytes(ipBytes);
        String ip = new String(ipBytes);
        int port = in.readInt();
        boolean success = in.readBoolean();
        byte[] identifyValBytes = new byte[36];
        in.readBytes(identifyValBytes);
        String identifyVal=new String(identifyValBytes);
        return new RoutingPacket(ip, port,success,identifyVal);
    }

    @ToString
    public static class RoutingPacket {
        public static byte failVal = 0x00;
        public static byte successVal = 0x01;
        public boolean success = false;
        public String desIp;
        public int desPort;
        public String identifyVal;

        /**
         * 生成专用
         * @param desIp
         * @param desPort
         */
        public RoutingPacket(String desIp, int desPort) {
            this.desIp = desIp;
            this.desPort = desPort;
            this.identifyVal=AcquireIdentifyVal();
        }

        /**
         * 解码专用
         * @param desIp
         * @param desPort
         * @param success
         */
        public RoutingPacket(String desIp, int desPort, boolean success,String identifyVal) {
            this.desIp = desIp;
            this.desPort = desPort;
            this.success = success;
            this.identifyVal=identifyVal;
        }


        public String getKey() {
            return desIp + " " + desPort+" "+this.identifyVal;
        }

        private String AcquireIdentifyVal() {
            return UUID.randomUUID().toString();
        }
    }

    public static void main(String[] args) {
        System.out.println(UUID.randomUUID().toString().getBytes().length);
    }
}
