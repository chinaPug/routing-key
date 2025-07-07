package cn.pug.client.component.init;

import cn.pug.common.inboundHandler.Client2DestInboundHandler;
import cn.pug.common.inboundHandler.Dest2ClientInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TcpInitialRequestInboundHandler extends SimpleChannelInboundHandler<String> {
    private EventLoopGroup proxyGroup;
    public TcpInitialRequestInboundHandler(EventLoopGroup proxyGroup) {
        this.proxyGroup=proxyGroup;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String socks5ServerMetadata) {
        log.info("收到代理服务元信息：{}", socks5ServerMetadata);
        // 获取SOCKS5命令请求的目的地址类型
        String[] socks5Server = socks5ServerMetadata.split("-");



        // 准备连接目标服务器，记录调试日志
        log.debug("准备连接代理服务，ip={},port={}", socks5Server[0],socks5Server[1]);

        // 配置客户端通道，以连接到目标服务器
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(this.proxyGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // 添加服务端写客户端的Handler
                        ch.pipeline().addLast(new Dest2ClientInboundHandler(ctx));
                    }
                });

        // 尝试连接目标服务器
        bootstrap.connect(socks5Server[0], Integer.parseInt(socks5Server[1]))
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        log.debug("目标服务器连接成功");
                        // 添加客户端转发请求到服务端的Handler
                        ctx.pipeline().addLast(new Client2DestInboundHandler(future));
                        // 发送成功的SOCKS5命令响应
//                        DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5AddressType);
//                        ctx.writeAndFlush(commandResponse);
                        // 移除不再需要的处理器
                        ctx.pipeline().remove(StringDecoder.class);
                        ctx.pipeline().remove(StringEncoder.class);
                    } else {
                        log.error("连接目标服务器失败,address={},port={}",  socks5Server[0],socks5Server[1]);
                        // 发送失败的SOCKS5命令响应
//                        DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socks5AddressType);
//                        ctx.writeAndFlush(commandResponse);
                        // 关闭连接失败的通道
                        future.channel().close();
                    }
                });
    }
}
