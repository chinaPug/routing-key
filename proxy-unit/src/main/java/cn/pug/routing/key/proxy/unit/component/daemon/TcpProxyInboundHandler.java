package cn.pug.routing.key.proxy.unit.component.daemon;

import cn.pug.routing.key.proxy.unit.component.inboundHandler.Client2DesInboundHandler;
import cn.pug.routing.key.proxy.unit.component.inboundHandler.Des2ClientInboundHandler;
import cn.pug.common.handler.ExceptionHandler;
import cn.pug.common.protocol.RoutingKeyProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TcpProxyInboundHandler extends SimpleChannelInboundHandler<String> {
    private EventLoopGroup proxyGroup;
    private UnitConfig unitConfig= UnitConfig.UnitConfigHolder.INSTANCE.getUnitConfig();

    public TcpProxyInboundHandler(EventLoopGroup proxyGroup) {
        this.proxyGroup = proxyGroup;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext toServerDaemonCtx, String desMsg) throws Exception {
        log.info("收到服务端发送的请求帮浏览器转发信息：{}", desMsg);
        String[] desMsgArray = desMsg.split(RoutingKeyProtocol.SEGMENT_SPLIT);
        if (desMsgArray.length==3){
            //创建一个客户端
            Bootstrap bootstrap = new Bootstrap()
                    .group(proxyGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new DaemonChannelInitializer());
            // 这里是服务端发来的三元组
            String desHostIp=desMsgArray[0];
            int desPort = Integer.parseInt(desMsgArray[1]);
            int proxyPort=Integer.parseInt(desMsgArray[2]);;
            // 尝试连接目标服务器
            log.info("目标服务器连接中【{}】", desHostIp+ RoutingKeyProtocol.SEGMENT_SPLIT +desPort);
            bootstrap.connect(desHostIp, desPort).addListener((ChannelFutureListener) future0 -> {
                String response;
                if (future0.isSuccess()) {
                    response = desMsg + "\r\n";
                    log.info("目标服务器连接成功:{}",desMsg);
                }else {
                    response = desMsg + RoutingKeyProtocol.SEGMENT_SPLIT +"false\r\n";
                    log.info("请求被拒绝:{}",desMsg);
                }

                bootstrap.connect(unitConfig.proxyConfig.ip, proxyPort).addListener((ChannelFutureListener) future1 -> {
                    if (future1.isSuccess()) {
                        log.info("代理服务连接成功");
                        Channel toDesChannel = future0.channel();
                        Channel toServerChannel = future1.channel();
                        toServerChannel.pipeline()
                                .addLast(new Client2DesInboundHandler(toDesChannel))
                                .addLast(new ExceptionHandler());
                        toDesChannel.pipeline()
                                .addLast(new Des2ClientInboundHandler(toServerChannel))
                                .addLast(new ExceptionHandler());
                        log.info("双向连接建立");
                        toServerChannel.writeAndFlush(response);
                        if (toDesChannel.pipeline().get(StringEncoder.class) != null)
                            toDesChannel.pipeline().remove(StringEncoder.class);
                    }
                });
            });
           
        }else {
            log.warn("无效代理请求，报文无法解析【{}】，该代理被忽略", desMsg);
        }
    }
}
