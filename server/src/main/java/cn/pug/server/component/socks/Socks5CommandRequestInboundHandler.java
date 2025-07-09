package cn.pug.server.component.socks;

import cn.pug.common.utils.NetUtil;
import cn.pug.server.component.ServerContext;
import cn.pug.server.component.inboundHandler.Browser2ServerInboundHandler;
import cn.pug.server.component.inboundHandler.Client2ServerInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.ReferenceCountUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.IntStream;


@Slf4j
public class Socks5CommandRequestInboundHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private EventLoopGroup bossGroup = new NioEventLoopGroup();
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
        int availablePort = IntStream.rangeClosed(9000, 10000)
                .filter(NetUtil::isPortCanUse)
                .findFirst().orElse(-1);
        ServerBootstrap clientProxyServerBootstrap = new ServerBootstrap()
                .group(this.bossGroup, this.workerGroup)
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
                });

        clientProxyServerBootstrap.bind(availablePort)
                .addListener(future0 -> {
                    if (future0.isSuccess()) {
                        // 通过与客户端daemon的tcp通道发送连接请求，有浏览器请求代理的目标地址和本次需要客户端主动连接的端口
                        socks5.getToClientDaemonChannel().writeAndFlush(msg.dstAddr() + "-" + msg.dstPort() + "-" + availablePort + "\r\n").addListener(future1 -> {
                            if (future1.isSuccess()) {
                                log.info("已成功向客户端发送代理转发请求");
                                // 移除不再需要的处理器，处理socks握手请求的处理器需要移除
                                toBrowserCtx.pipeline().remove(Socks5ServerEncoder.DEFAULT);
                                // 等待客户端回传与目标服务器连接成功的信息
                                socks5.des2toBrowserCtxMap.put(msg.dstAddr() + "-" + msg.dstPort() + "-" + availablePort, toBrowserCtx);
                                // 获取SOCKS5命令请求的目的地址类型
                                socks5.des2toSocks5AddressTypeMap.put(msg.dstAddr() + "-" + msg.dstPort() + "-" + availablePort, msg.dstAddrType());
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

/**
 * 成功创建 Socks5 代理
 * DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5AddressType);
 * toBrowserCtx.writeAndFlush(commandResponse);
 * 失败 创建 Socks5 代理
 * DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socks5AddressType);
 * toBrowserCtx.writeAndFlush(commandResponse);
 */