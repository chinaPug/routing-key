package cn.pug.common.protocol;

/**
 * 协议报文为固定格式+变长
 * 固定格式为 魔数(8:0xAC)+版本号(4:0x1)+变长
 */
public class RoutingKeyProtocol {
    // 段分隔符
    public static final String SEGMENT_SPLIT = " ";

}
