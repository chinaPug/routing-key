package cn.pug.server.component.daemon;


import cn.pug.server.component.ServerContext;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Daemon {
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final ServerBootstrap bootstrap = new ServerBootstrap();
    private final int port;

    public Daemon(int port) {
        this.port = port;
    }

    public void start() {
        try {
            // daemon-serverBootstrap
            this.bootstrap
                    .group(this.bossGroup, this.workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new DaemonChannelInitializer())
                    .bind(this.port).sync().addListener(channelFuture -> {
                        if (channelFuture.isSuccess()) {
                            log.info("代理守护服务启动成功，监听端口【{}】", this.port);
                        } else {
                            log.error("代理守护服务启动失败，端口【{}】绑定异常", this.port, channelFuture.cause());
                        }
                    }).channel().closeFuture().sync().addListener(channelFuture -> {
                log.info("代理守护服务正在关闭");
                ServerContext.getInstance().shutdownGracefully();
            });
        } catch (Exception e) {
            log.error("代理守护服务启动异常", e);
        } finally {
            log.info("代理守护服务停止");
            this.shutdownGracefully();
            // 停止服务
            ServerContext.getInstance().shutdownGracefully();
        }
    }

    public void shutdownGracefully() {
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
        log.info("代理守护服务停止");
    }
}


