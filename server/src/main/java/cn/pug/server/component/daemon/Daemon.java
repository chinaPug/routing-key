package cn.pug.server.component.daemon;


import cn.pug.server.component.ServerContext;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Daemon {
    private final EventLoopGroup bossGroup= new NioEventLoopGroup();
    private final EventLoopGroup workerGroup= new NioEventLoopGroup();
    private final ServerBootstrap bootstrap= new ServerBootstrap();
    private final int port;

    public Daemon(int port) {
        this.port = port;
    }
    public void start() {
        try {
            this.bootstrap
                    .group(this.bossGroup,this.workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new DaemonChannelInitializer())
                    .bind(this.port).sync()
                    .addListener(channelFuture -> {
                        if (channelFuture.isSuccess()) {
                            log.info("代理守护服务启动成功，监听端口【{}】 ", this.port);
                        }
                    }).channel().closeFuture().sync().addListener(channelFuture -> {
                        log.info("代理守护服务已关闭");
                        ServerContext.getInstance().removeSocksProxy(this.port);

                    });
        } catch (Exception e) {
            log.error("代理守护服务启动异常",e);
        }finally {
            this.stop();
            // 停止服务
            ServerContext.getInstance().stop();
        }
    }

    public void stop() {
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
        log.info("代理守护服务停止");
    }
}


