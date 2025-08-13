package cn.pug.routing.key.proxy.core.protocol.decoder.parser;

import cn.pug.routing.key.proxy.core.protocol.RoutingKeyProtocol;
import cn.pug.routing.key.proxy.core.protocol.decoder.Parser;
import io.netty.buffer.ByteBuf;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;




/**
 * 路由报文解析器
 * <p>
 * 作用：路由请求和响应报文解析
 *
 * 协议报文格式:
 * <pre>
 * +---------------------------------------------------------------+
 * | 魔数(1byte) | 消息类型(1byte) | 数据体(可变长字节)             |
 * +---------------------------------------------------------------+
 * 数据体长度根据具体消息类型确定，可能为固定长度或可变长度。
 * 对于路由请求和响应，数据体格式如下：
 * +---------------------------------------------------------------+
 * | IP长度(4byte) | IP地址(可变长字节) | 端口(4byte) | 成功标识(1byte) | 标识值(36byte) |
 * +---------------------------------------------------------------+
 * 其中：
 * - IP长度：占用4个字节，表示后续IP地址的字节长度；
 * - IP地址：长度由前面的IP长度字段决定，用于存储目标主机的IP地址；
 * - 端口：占用4个字节，表示目标主机的端口号；
 * - 成功标识：占用1个字节，0x00表示失败，0x01表示成功；
 * - 标识值：固定36个字节，用于唯一标识本次路由操作，通常为UUID字符串；
 * </pre>
 *
 * @author pug
 * @since 1.0.0
 */
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
}
