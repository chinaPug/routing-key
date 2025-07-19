package cn.pug.common.protocol.parser;

import cn.pug.common.protocol.RoutingKeyProtocol;
import io.netty.buffer.ByteBuf;

import java.util.Set;

public interface Parser {
    Set<RoutingKeyProtocol.State> support();
    // 是否支持指定状态
    boolean isSupport(RoutingKeyProtocol.State state);

    void addSupport(RoutingKeyProtocol.State state);
    // 将比特数组转具体的报文实体类
    Object parser(ByteBuf in);
}
