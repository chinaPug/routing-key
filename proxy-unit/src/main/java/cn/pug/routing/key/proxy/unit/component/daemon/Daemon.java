package cn.pug.routing.key.proxy.unit.component.daemon;

import cn.pug.common.protocol.RegisterRequestEncoder;
import cn.pug.common.protocol.RoutingKeyDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Daemon {
    private EventLoopGroup bossGroup = new NioEventLoopGroup();
    public EventLoopGroup proxyGroup = new NioEventLoopGroup();
    private Bootstrap bootstrap = new Bootstrap();
    public UnitConfig unitConfig= UnitConfig.UnitConfigHolder.INSTANCE.getUnitConfig();
    public int socksPort;

    public void start() {
        try {
            this.bootstrap
                    .group(this.bossGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    // routing key协议编码器
                                    .addLast(new RoutingKeyDecoder())
                                    // 注册请求编码器
                                    .addLast(new RegisterRequestEncoder())
                                    // 注册入站处理器
                                    .addLast(new RegisterInboundHandler(Daemon.this));
                        }
                    });
            // 连接proxy pool
            Channel channel = this.bootstrap.connect(unitConfig.proxyConfig.ip, unitConfig.proxyConfig.port).sync().channel();
            // 阻塞等待关闭
            channel.closeFuture().sync();

        } catch (Exception e) {
            log.error("客户端初始化服务启动异常", e);
        } finally {
            this.bossGroup.shutdownGracefully();
            this.proxyGroup.shutdownGracefully();
        }
    }
}
