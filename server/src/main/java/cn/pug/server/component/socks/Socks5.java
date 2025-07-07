package cn.pug.server.component.socks;

import cn.pug.common.utils.NetUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5 {
    // 负责等待连接的线程池
    private EventLoopGroup bossGroup;
    // 负责处理连接的线程池
    private EventLoopGroup workerGroup;
    // 负责对接客户端的线程池
    private EventLoopGroup porxyGroup;

    private ServerBootstrap bootstrap;

    private int port;

    public Socks5(int port) {
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.porxyGroup = new NioEventLoopGroup();
        this.bootstrap = new ServerBootstrap();
        this.port = port;
    }

    public void start(ChannelHandlerContext ctx) {
        try {

            ChannelFuture channelFuture = this.bootstrap
                    // 设置线程组
                    .group(this.bossGroup, this.workerGroup)
                    // 设置管道是nio的
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    // 添加Socks5编码器
                                    .addLast(Socks5ServerEncoder.DEFAULT)
                                    // 处理Socks5初始化请求
                                    .addLast(new Socks5InitialRequestDecoder())
                                    .addLast(new Socks5InitialRequestInboundHandler())
                                    // 处理连接请求
                                    .addLast(new Socks5CommandRequestDecoder())
                                    .addLast(new Socks5CommandRequestInboundHandler(porxyGroup));
                        }
                    }).bind(this.port).sync();
            if (channelFuture.isSuccess()) {
                // 在上下文中添加该代理
//                ServerContext.getInstance().registrySocksProxy(this.port, this);
                // 4 返回注册结果
                log.info("返回代理服务元信息【{}】 ", NetUtil.getLocalIP().concat("-").concat(String.valueOf(this.port)));
                ctx.writeAndFlush(NetUtil.getLocalIP().concat("-").concat(String.valueOf(this.port)));
                log.info("Socks5代理服务启动成功，监听端口【{}】 ", this.port);
            }
            // 等待服务器关闭
            channelFuture.channel().closeFuture().sync().addListener(channelFuture0 -> {
                /// 在上下文中移除该代理
                log.info("Socks5代理服务已关闭");
//                ServerContext.getInstance().removeSocksProxy(this.port);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // 关闭线程组
            this.stop();
        }
    }

    public void stop() {
        // 关闭线程组
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
        this.porxyGroup.shutdownGracefully();
    }
}
