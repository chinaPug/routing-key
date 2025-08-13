package cn.pug.routing.key.proxy.unit.daemon;

import cn.pug.routing.key.proxy.core.handler.ExceptionHandler;
import cn.pug.routing.key.proxy.core.protocol.encoder.RegisterRequestEncoder;
import cn.pug.routing.key.proxy.core.protocol.decoder.RoutingKeyDecoder;
import cn.pug.routing.key.proxy.unit.config.UnitConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class Daemon {
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup proxyGroup = new NioEventLoopGroup();
    private final Bootstrap bootstrap = new Bootstrap();
    private final UnitConfig unitConfig= UnitConfig.UnitConfigHolder.INSTANCE.getUnitConfig();
    @Setter
    private int socksPort;

    public void start() {
        try {
            this.bootstrap
                    .group(this.bossGroup)
                    .channel(NioSocketChannel.class)
                    .option(io.netty.channel.ChannelOption.SO_KEEPALIVE, true)
                    .option(io.netty.channel.ChannelOption.TCP_NODELAY, true)
                    .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addFirst(new ExceptionHandler())
                                    .addLast(new RoutingKeyDecoder())
                                    .addLast(new RegisterRequestEncoder())
                                    .addLast(new RegisterInboundHandler(Daemon.this));
                        }
                    });
            
            // 连接proxy pool
            log.info("正在连接到代理池: {}:{}", unitConfig.getProxyConfig().getIp(), unitConfig.getProxyConfig().getPort());
            Channel channel = this.bootstrap.connect(unitConfig.getProxyConfig().getIp(), unitConfig.getProxyConfig().getPort())
                    .sync().channel();
            log.info("成功连接到代理池");
            
            // 阻塞等待关闭
            channel.closeFuture().sync();

        } catch (InterruptedException e) {
            log.error("连接被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("客户端初始化服务启动异常", e);
        } finally {
            shutdownGracefully();
        }
    }
    
    public void shutdownGracefully() {
        log.info("开始关闭代理单元服务...");
        
        if (!this.bossGroup.isShutdown()) {
            this.bossGroup.shutdownGracefully().syncUninterruptibly();
        }
        if (!this.proxyGroup.isShutdown()) {
            this.proxyGroup.shutdownGracefully().syncUninterruptibly();
        }
        
        log.info("代理单元服务已停止");
    }
}
