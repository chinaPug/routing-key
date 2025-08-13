package cn.pug.routing.key.proxy.core.protocol.decoder;

import cn.pug.routing.key.proxy.core.protocol.RoutingKeyProtocol;
import io.netty.buffer.ByteBuf;

import java.util.Set;
/**
 * 报文解析器接口
 *
 * @author pug
 * @since 1.0.0
 */
public interface Parser {
    Set<RoutingKeyProtocol.State> support();
    // 是否支持指定状态
    boolean isSupport(RoutingKeyProtocol.State state);

    void addSupport(RoutingKeyProtocol.State state);
    // 将比特数组转具体的报文实体类
    Object parser(ByteBuf in);
}
