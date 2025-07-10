package cn.pug.server.component.socks;

import cn.pug.common.protocol.RoutingKeyProtocol;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.ReferenceCountUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.stream.IntStream;


@Slf4j
public class Socks5CommandRequestInboundHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

    private Socks5 socks5;

    public Socks5CommandRequestInboundHandler(Socks5 socks5) {
        this.socks5 = socks5;
    }

    @Override
    @SneakyThrows
    protected void channelRead0(ChannelHandlerContext toBrowserCtx, DefaultSocks5CommandRequest msg) {

        // 检查请求类型是否为CONNECT，如果不是，则记录日志并继续处理其他读取事件
        if (!msg.type().equals(Socks5CommandType.CONNECT)) {
            log.info("接收commandRequest类型【{}】，非CONNECT类型，终止连接", msg.type());
            ReferenceCountUtil.retain(msg);
            toBrowserCtx.fireChannelRead(msg);
            return;
        }

        // 准备连接目标服务器，记录调试日志
        log.debug("准备连接目标服务器，ip={},port={}", msg.dstAddr(), msg.dstPort());
        // 发送这个当前浏览器的请求报文到tcp服务器
        //启动监听
        new ServerBootstrap()
                .group( new NioEventLoopGroup(2))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()))
                                .addLast(new StringDecoder())
                                .addLast(new StringEncoder())
                                .addLast(new Wait(socks5));
                    }
                }).bind(0)
                .addListener((ChannelFutureListener)future0 -> {
                    if (future0.isSuccess()) {
                        // 通过与客户端daemon的tcp通道发送连接请求，有浏览器请求代理的目标地址和本次需要客户端主动连接的端口
                        int port = ((InetSocketAddress) future0.channel().localAddress()).getPort();
                        socks5.getToClientDaemonChannel().writeAndFlush(msg.dstAddr() + RoutingKeyProtocol.SEGMENT_SPLIT + msg.dstPort() + RoutingKeyProtocol.SEGMENT_SPLIT + port + "\r\n").addListener(future1 -> {
                            if (future1.isSuccess()) {
                                log.info("已成功向客户端发送代理转发请求");
                                // 等待客户端回传与目标服务器连接成功的信息
                                socks5.des2toBrowserCtxMap.put(msg.dstAddr() + RoutingKeyProtocol.SEGMENT_SPLIT + msg.dstPort() + RoutingKeyProtocol.SEGMENT_SPLIT + port, toBrowserCtx);
                                // 放入SOCKS5命令请求的目的地址类型
                                socks5.des2toSocks5AddressTypeMap.put(msg.dstAddr() + RoutingKeyProtocol.SEGMENT_SPLIT + msg.dstPort() + RoutingKeyProtocol.SEGMENT_SPLIT + port, msg.dstAddrType());
                            } else {
                                log.error("连接目标服务器失败,address={},port={}", msg.dstAddr(), msg.dstPort());
                            }
                        });
                    } else {
                        log.error("client proxy绑定端口失败");
                    }
                });
        log.info("client proxy启动成功");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("xxxx", cause);
    }
}
