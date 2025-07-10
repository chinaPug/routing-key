package cn.pug.routing.key.proxy.pool.component.daemon;


import cn.pug.routing.key.proxy.pool.component.ServerContext;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

// 1 该服务为daemon服务，在该业务逻辑单元会处理以下几件事：
// 1.1 开放一个tcp端口，接收客户端连接，相当于服务注册请求的处理；
// 1.2 接收到服务注册报文后，需要在代理池约定的端口范围内，找到一个可用的端口并创建一个socks服务，准备接收连接
// 1.3 将创建的socks服务的端口号发送给客户端，并保存该端口号与客户端之间的映射关系，用于后续的连接转发
// 到此为止，该daemon服务的任务完成，总结：负责处理客户端代理服务注册和分配本地代理服务相关信息
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
            // 守护进程服务
            this.bootstrap
                    .group(this.bossGroup, this.workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new DaemonChannelInitializer())
                    .bind(this.port).sync().addListener(future -> {
                        if (future.isSuccess()) {
                            log.info("代理守护服务启动成功，监听端口【{}】", this.port);
                        } else {
                            log.error("代理守护服务启动失败，端口【{}】绑定异常", this.port, future.cause());
                        }
                    }).channel().closeFuture().sync().addListener(future -> {
                        log.info("代理守护服务正在关闭");
                        this.shutdownGracefully();
                    });
        } catch (Exception e) {
            log.error("代理守护服务启动异常", e);
        } finally {
            log.info("代理守护服务停止");
            this.shutdownGracefully();
        }
    }

    public void shutdownGracefully() {
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
        // 守护进程停止要调用整个上下文的socks服务销毁
        ServerContext.getInstance().shutdownGracefully();
        log.info("代理守护服务停止");
    }
}


