package cn.pug.routing.key.proxy.unit.daemon;

import cn.pug.common.handler.ExceptionHandler;
import cn.pug.common.handler.P2PInboundHandler;
import cn.pug.common.protocol.RoutingResponseEncoder;
import cn.pug.common.protocol.parser.RoutingParser;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoutingInboundHandler extends SimpleChannelInboundHandler<RoutingParser.RoutingPacket> {
    private final Daemon daemon;
    public RoutingInboundHandler(Daemon daemon) {
        this.daemon=daemon;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RoutingParser.RoutingPacket routingPacket) throws Exception {
        //创建一个客户端
        Bootstrap bootstrap = new Bootstrap()
                .group(daemon.getProxyGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new RoutingResponseEncoder())
                                // 添加异常处理
                                .addFirst(new ExceptionHandler());
                    }
                });
        // 获取需要转发的地址
        String desIp=routingPacket.desIp;
        int desPort = routingPacket.desPort;
        // 获取对应池中的位置
        int socksPort=daemon.getSocksPort();
        // 尝试连接目标服务器
        log.info("目标服务器连接中【{}】", desIp+" " +desPort);
        bootstrap.connect(desIp, desPort).addListener((ChannelFutureListener) future0 -> {
            // 判断与目标地址是否连接成功
            if (future0.isSuccess()) {
                routingPacket.success=true;
                log.info("目标服务器连接成功:{}",routingPacket);
            }else {
                routingPacket.success=false;
                log.info("请求被拒绝:{}",routingPacket);
            }
            // 连接socks5服务
            bootstrap.connect(daemon.getUnitConfig().getProxyConfig().getIp(), socksPort).addListener((ChannelFutureListener) future1 -> {
                if (future1.isSuccess()) {
                    log.info("socks5服务连接成功");
                    Channel toDesChannel = future0.channel();
                    Channel toServerChannel = future1.channel();
                    toServerChannel.pipeline()
                            .addLast(new P2PInboundHandler(toDesChannel));
                    toDesChannel.pipeline()
                            .addLast(new P2PInboundHandler(toServerChannel));
                    log.info("双向连接建立");
                    // 向socks服务发送准备就绪请求
                    toServerChannel.writeAndFlush(routingPacket).addListener(future2->{
                        if(toServerChannel.pipeline().get(RoutingResponseEncoder.class) != null) {
                            toServerChannel.pipeline().remove(RoutingResponseEncoder.class);
                        }
                    });
                }
            });
        });
    }
}
