package cn.pug.routing.key.proxy.pool.component.socks.handler;

import cn.pug.routing.key.proxy.core.protocol.decoder.parser.RoutingParser;
import cn.pug.routing.key.proxy.pool.component.socks.Socks5;
import cn.pug.routing.key.proxy.pool.component.socks.pojo.SocksPacketKeeper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.ReferenceCountUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Socks5CommandRequestInboundHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {
    // 该channel所属的socks5服务
    private final Socks5 socks5;

    public Socks5CommandRequestInboundHandler(Socks5 socks5) {
        this.socks5 = socks5;
    }

    @Override
    @SneakyThrows
    protected void channelRead0(ChannelHandlerContext toClientCtx, DefaultSocks5CommandRequest msg) {
        // 检查请求类型是否为CONNECT，如果不是，则记录日志并继续处理其他读取事件
        if (!msg.type().equals(Socks5CommandType.CONNECT)) {
            log.info("接收commandRequest类型【{}】，非CONNECT类型，终止连接", msg.type());
            ReferenceCountUtil.retain(msg);
            toClientCtx.fireChannelRead(msg);
            return;
        }

        // 发送这个当前浏览器的请求报文到tcp服务器
        log.info("接收commandRequest:目标ip【{}】，目标port【{}】",msg.dstAddr(), msg.dstPort());
        RoutingParser.RoutingPacket routingPacket = new RoutingParser.RoutingPacket(msg.dstAddr(), msg.dstPort());
        socks5.toUnitDaemon.channel().writeAndFlush(routingPacket).addListener(future -> {
            if (future.isSuccess()){
                log.info("通知unit转发消息发送成功:【{}】",routingPacket);
                // 失败则直接返回给浏览器
                SocksPacketKeeper socksPacketKeeper=new SocksPacketKeeper(routingPacket, toClientCtx, msg.dstAddrType());
                // 把浏览器的信息放入map中，方便后续使用
                socks5.socksPacketKeeperConcurrentHashMap.put(socksPacketKeeper.getKey(),socksPacketKeeper);
                ChannelPipeline pipeline = toClientCtx.pipeline();
                // 移除握手相关的内容，只保留一个无处理器的tcp连接
                pipeline.remove(Socks5CommandRequestInboundHandler.class);
            }else {
                log.info("通知unit转发消息发送失败");
                // 失败则直接返回给浏览器
                DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, msg.dstAddrType());
                toClientCtx.writeAndFlush(commandResponse);
            }
        });
    }

}