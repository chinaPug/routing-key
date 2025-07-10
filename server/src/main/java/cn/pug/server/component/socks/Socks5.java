package cn.pug.server.component.socks;

import cn.pug.common.handler.ExceptionHandler;
import cn.pug.common.protocol.RoutingKeyProtocol;
import cn.pug.server.component.ServerContext;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
public class Socks5 {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap browserProxyServerBootstrap;
    private int browserProxyPort;
    // 目标地址到ChannelHandlerContext toBrowserCtx
    public Map<String, ChannelHandlerContext> des2toBrowserCtxMap = new ConcurrentHashMap<>(32);
    // 目标地址到Socks5AddressType
    public Map<String, Socks5AddressType> des2toSocks5AddressTypeMap = new ConcurrentHashMap<>(32);

    private Channel toClientDaemonChannel;

    public Socks5(Channel toClientDaemonChannel) {
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.browserProxyServerBootstrap = new ServerBootstrap();
        this.toClientDaemonChannel = toClientDaemonChannel;
    }

    public void start() {
        try {
            // 配置browser proxy服务端
            // 该服务端会承接浏览器和代理客户端之间的连接
            this.browserProxyServerBootstrap
                    .group(this.bossGroup, this.workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 添加Socks协议解码编码器
                            ch.pipeline()
                                    // 添加Socks5编码器
                                    .addLast(Socks5ServerEncoder.DEFAULT)
                                    // 处理Socks5初始化请求
                                    .addLast(new Socks5InitialRequestDecoder())
                                    .addLast(new Socks5InitialRequestInboundHandler())
                                    // 处理连接请求
                                    .addLast(new Socks5CommandRequestDecoder())
                                    .addLast(new Socks5CommandRequestInboundHandler(Socks5.this))
                                    .addLast(new ExceptionHandler());
                        }
                    }).bind(0).sync()
                    .addListener((ChannelFutureListener) future -> {
                        // browser proxy服务端启动成功
                        if (future.isSuccess()) {
                            // 获取端口号
                            this.browserProxyPort = ((InetSocketAddress) future.channel().localAddress()).getPort();
                            log.info("Socks5代理服务启动成功，监听端口【{}】", this.browserProxyPort);
                            // 在上下文中注册该代理
                            ServerContext.getInstance().registrySocksProxy(this.browserProxyPort, this);
                            // 获取browser proxy服务端坐标发送给客户端
                            String localAddress = ServerContext.getInstance().getIp();
                            String proxyInfo = localAddress + RoutingKeyProtocol.SEGMENT_SPLIT + this.browserProxyPort + "\r\n";
                            log.info("browser proxy服务端启动成功，下一步发送元数据【{}】到客户端", proxyInfo);
                            this.toClientDaemonChannel.writeAndFlush(proxyInfo);
                        } else {
                            log.error("Socks5代理服务启动失败");
                            this.shutdownGracefully();
                        }
                    }).channel().closeFuture().addListener(future -> {
                        log.info("Socks5代理服务正在关闭");
                        this.shutdownGracefully();

                    });

        } catch (Exception e) {
            log.error("启动Socks5代理服务时发生异常", e);
            this.shutdownGracefully();
        }
    }

    public void shutdownGracefully() {
        // 关闭线程组
        log.info("正在停止代理服务...");
        ServerContext.getInstance().removeSocksProxy(this.browserProxyPort);
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }

}


