package cn.pug.client.component.init;

import cn.pug.client.component.inboundHandler.Client2DesInboundHandler;
import cn.pug.client.component.inboundHandler.Des2ClientInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TcpProxyInboundHandler extends SimpleChannelInboundHandler<String> {
    private EventLoopGroup proxyGroup;
    private String hostIp;
    public TcpProxyInboundHandler(EventLoopGroup proxyGroup, String hostIp) {
        this.proxyGroup = proxyGroup;
        this.hostIp = hostIp;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext toServerDaemonCtx, String desMsg) throws Exception {
        log.info("收到服务端发送的请求帮浏览器转发信息：{}", desMsg);
        String[] desMsgArray = desMsg.split("-");
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
            log.info("目标服务器连接中【{}】", desHostIp+"-"+desPort);
            bootstrap.connect(desHostIp, desPort).addListener((ChannelFutureListener) future0 -> {
                String response;
                if (future0.isSuccess()) {
                    response = desMsg + "\r\n";
                    log.info("目标服务器连接成功");
                }else {
                    response = desMsg +"-false\r\n";
                    log.info("请求被拒绝");
                }

                bootstrap.connect(hostIp, proxyPort).addListener((ChannelFutureListener) future1 -> {
                    if (future1.isSuccess()) {
                        log.info("代理服务连接成功");
                        Channel toDes = future0.channel();
                        Channel toServer = future1.channel();
                        toServer.pipeline()
                                .addLast(new Client2DesInboundHandler(toDes));
                        toDes.pipeline()
                                .addLast(new Des2ClientInboundHandler(toServer));
                        log.info("双向连接建立");
                        toServer.writeAndFlush(response);
                        toDes.pipeline().remove(StringEncoder.class);

                    }
                });
            });
           
        }else {
            log.warn("无效代理请求，报文无法解析【{}】，该代理被忽略", desMsg);
        }
    }
}
