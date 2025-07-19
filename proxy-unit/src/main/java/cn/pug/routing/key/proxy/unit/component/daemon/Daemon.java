package cn.pug.routing.key.proxy.unit.component.daemon;

import cn.pug.common.protocol.RegisterRequestEncoder;
import cn.pug.common.protocol.RegisterResponseEncoder;
import cn.pug.common.protocol.RoutingKeyDecoder;
import cn.pug.common.protocol.parser.RegisterParser;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.bootstrap.Bootstrap;
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
                                    .addLast(new RoutingKeyDecoder())
                                    .addLast(new RegisterRequestEncoder())
                                    .addLast(new RegisterInboundHandler(Daemon.this));
                        }
                    });
            Channel channel = this.bootstrap.connect(unitConfig.proxyConfig.ip, unitConfig.proxyConfig.port).sync().channel();
            // 发送注册信息
            channel.writeAndFlush("").addListener(
                    future -> {
                        if (future.isSuccess()) {
                            log.info("发送注册请求成功");
                        } else {
                            log.error("发送注册请求失败");
                        }
                    }
            );
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
