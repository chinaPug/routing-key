package cn.pug.routing.key.proxy.pool.component.daemon;


import cn.pug.common.handler.ExceptionHandler;
import cn.pug.common.protocol.encoder.RegisterResponseEncoder;
import cn.pug.common.protocol.decoder.RoutingKeyDecoder;
import cn.pug.routing.key.proxy.pool.component.daemon.handler.UnitRegisterInbounderHandler;
import cn.pug.routing.key.proxy.pool.component.socks.handler.ConnectionStatisticsHandler;
import cn.pug.routing.key.proxy.pool.component.socks.Socks5;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Getter
public class Daemon {
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final ServerBootstrap bootstrap = new ServerBootstrap();
    private final int port;
    private final Map<SocketAddress, Socks5> proxyUnitMap=new ConcurrentHashMap<>();

    public Daemon(int port) {
        this.port = port;
    }

    public void start() {
        try {
            // 守护进程服务
            this.bootstrap
                    .group(this.bossGroup, this.workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new RoutingKeyDecoder())
                                    .addLast(new RegisterResponseEncoder())
                                    .addLast(new UnitRegisterInbounderHandler(Daemon.this))
                                    .addFirst(new ConnectionStatisticsHandler(Daemon.this))
                                    .addFirst(new ExceptionHandler());
                        }
                    })
                    .bind(this.port).sync().addListener(future -> {
                        if (future.isSuccess()) {
                            log.info("代理守护服务启动成功，监听端口【{}】", this.port);
                        } else {
                            log.error("代理守护服务启动失败，端口【{}】绑定异常", this.port, future.cause());
                        }
                    }).channel()
                    .closeFuture().sync().addListener(future -> {
                        log.info("代理守护服务正在关闭");
                        this.shutdownGracefully();
                    });
        } catch (Exception e) {
            log.error("代理守护服务启动异常", e);
        } finally {
            this.shutdownGracefully();
        }
    }

    public void shutdownGracefully() {
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
        log.info("代理守护服务停止");
        System.exit(-1);
    }
}


