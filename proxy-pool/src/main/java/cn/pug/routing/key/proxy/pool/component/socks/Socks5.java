package cn.pug.routing.key.proxy.pool.component.socks;

import cn.pug.common.handler.ExceptionHandler;
import cn.pug.common.protocol.RegisterResponseEncoder;
import cn.pug.common.protocol.RoutingKeyProtocol;
import cn.pug.common.protocol.RoutingRequestEncoder;
import cn.pug.common.protocol.parser.RegisterParser;
import cn.pug.routing.key.proxy.pool.component.ServerContext;
import cn.pug.routing.key.proxy.pool.component.daemon.UnitRegisterInbounderHandler;
import cn.pug.routing.key.proxy.pool.component.socks.pojo.SocksPacketKeeper;
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
    private ServerContext serverContext = ServerContext.ServerContextHolder.INSTANCE.getServerContext();
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap browserProxyServerBootstrap;
    private int browserProxyPort;
    // 目标地址到ChannelHandlerContext toBrowserCtx
    public Map<String, SocksPacketKeeper> socksPacketKeeperConcurrentHashMap = new ConcurrentHashMap<>(32);

    public ChannelHandlerContext toClientDaemon;

    public Socks5(ChannelHandlerContext toClientDaemon, int browserProxyPort) {
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.browserProxyServerBootstrap = new ServerBootstrap();
        this.toClientDaemon = toClientDaemon;
        this.browserProxyPort = browserProxyPort;
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
                                    .addLast(new SplitFlowInboundHandler(Socks5.this));
                        }
                    })
                    .bind(browserProxyPort).sync()
                    .addListener((ChannelFutureListener) future0 -> {
                        // browser proxy服务端启动成功
                        if (future0.isSuccess()) {
                            log.info("Socks5代理服务启动成功，监听端口【{}】", this.browserProxyPort);
                            this.toClientDaemon.writeAndFlush(new RegisterParser.RegisterPacket(this.browserProxyPort, true)).addListener((ChannelFutureListener) future1 -> {
                                if (future1.isSuccess()) {
                                    // 在上下文中注册该代理
                                    serverContext.registrySocksProxy(this.browserProxyPort, this);
                                    // 移除元数据编码器
                                    toClientDaemon.pipeline().remove(RegisterResponseEncoder.class);
                                    toClientDaemon.pipeline().remove(UnitRegisterInbounderHandler.class);
                                    toClientDaemon.pipeline().addLast(new RoutingRequestEncoder());
                                } else {
                                    // 说明此时与客户端的通道已经关闭，需要销毁该Socks对象
                                    log.error("向客户端发送元数据失败，销毁该通道");
                                    future1.channel().close().sync();
                                    this.shutdownGracefully();
                                }
                            });
                        } else {
                            log.error("Socks5代理服务启动失败");
                            this.shutdownGracefully();
                        }
                    }).channel().closeFuture().addListener(future0 -> {
                        log.info("Socks5代理服务正在关闭");
                        this.shutdownGracefully();

                    });

        } catch (Exception e) {
            log.error("启动Socks5代理服务时发生异常", e);
            this.shutdownGracefully();
        }
    }

    public void socksPacketKeeperConcurrentHashMapPrinter() {
        log.info("socksPacketKeeperConcurrentHashMap监控输出--start");
        log.info("socksPacketKeeperConcurrentHashMap size:{}", socksPacketKeeperConcurrentHashMap.size());
        socksPacketKeeperConcurrentHashMap.forEach((key, value) -> {
            log.info("key:{},value:{}", key, value);
        });
        log.info("socksPacketKeeperConcurrentHashMap监控输出--end");
    }

    public void shutdownGracefully() {
        // 关闭线程组
        log.info("正在停止代理服务...");
        serverContext.removeSocksProxy(this.browserProxyPort);
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }

}


