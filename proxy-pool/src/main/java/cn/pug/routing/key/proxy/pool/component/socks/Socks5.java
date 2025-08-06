package cn.pug.routing.key.proxy.pool.component.socks;

import cn.pug.common.handler.ExceptionHandler;
import cn.pug.common.protocol.RegisterResponseEncoder;
import cn.pug.common.protocol.RoutingRequestEncoder;
import cn.pug.common.protocol.parser.RegisterParser;
import cn.pug.routing.key.proxy.pool.component.ServerContext;
import cn.pug.routing.key.proxy.pool.component.daemon.Daemon;
import cn.pug.routing.key.proxy.pool.component.daemon.UnitRegisterInbounderHandler;
import cn.pug.routing.key.proxy.pool.component.socks.pojo.SocksPacketKeeper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
public class Socks5 {
    private ServerContext serverContext = ServerContext.ServerContextHolder.INSTANCE.getServerContext();
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap clientProxyServerBootstrap;
    private int clientProxyPort;
    // 目标地址到ChannelHandlerContext toClientCtx
    public Map<String, SocksPacketKeeper> socksPacketKeeperConcurrentHashMap = new ConcurrentHashMap<>(32);
    private final Daemon daemon;
    public ChannelHandlerContext toUnitDaemon;

    public Socks5(ChannelHandlerContext toUnitDaemon, int clientProxyPort, Daemon daemon) {
        this.daemon = daemon;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.clientProxyServerBootstrap = new ServerBootstrap();
        this.toUnitDaemon = toUnitDaemon;
        this.clientProxyPort = clientProxyPort;
    }

    public void start() {
        try {
            // 配置client proxy服务端
            // 该服务端会承接浏览器和代理客户端之间的连接
            this.clientProxyServerBootstrap
                    .group(this.bossGroup, this.workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 添加Socks协议解码编码器
                            ch.pipeline()
                                    // 添加Socks5编码器
                                    .addLast(new SplitFlowInboundHandler(Socks5.this))
                                    .addFirst(new ExceptionHandler());
                        }
                    })
                    .bind(clientProxyPort).sync()
                    .addListener((ChannelFutureListener) future0 -> {
                        // client proxy服务端启动成功
                        if (future0.isSuccess()) {
                            log.info("Socks5代理服务启动成功，监听端口【{}】", this.clientProxyPort);
                            this.toUnitDaemon.writeAndFlush(new RegisterParser.RegisterPacket(this.clientProxyPort, true)).addListener((ChannelFutureListener) future1 -> {
                                if (future1.isSuccess()) {
                                    // 在上下文中注册该代理
                                    serverContext.registrySocksProxy(this.clientProxyPort, this);
                                    // 移除元数据编码器
                                    toUnitDaemon.pipeline().remove(RegisterResponseEncoder.class);
                                    toUnitDaemon.pipeline().remove(UnitRegisterInbounderHandler.class);
                                    toUnitDaemon.pipeline().addLast(new RoutingRequestEncoder());
                                    daemon.getProxyUnitMap().put(toUnitDaemon.channel().remoteAddress(), Socks5.this);
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
        socksPacketKeeperConcurrentHashMap.clear();
        serverContext.removeSocksProxy(this.clientProxyPort);
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }

}


