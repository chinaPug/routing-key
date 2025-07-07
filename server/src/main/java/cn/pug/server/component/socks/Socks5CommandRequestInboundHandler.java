package cn.pug.server.component.socks;

import cn.pug.common.inboundHandler.Client2DestInboundHandler;
import cn.pug.common.inboundHandler.Dest2ClientInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Socks5CommandRequestInboundHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

    private EventLoopGroup proxyGroup;

    public Socks5CommandRequestInboundHandler(EventLoopGroup proxyGroup) {
        this.proxyGroup = proxyGroup;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg) {
        // 获取SOCKS5命令请求的目的地址类型
        Socks5AddressType socks5AddressType = msg.dstAddrType();

        // 检查请求类型是否为CONNECT，如果不是，则记录日志并继续处理其他读取事件
        if (!msg.type().equals(Socks5CommandType.CONNECT)) {
            log.info("接收commandRequest类型【{}】，非CONNECT类型，终止连接", msg.type());
            ReferenceCountUtil.retain(msg);
            ctx.fireChannelRead(msg);
            return;
        }

        // 准备连接目标服务器，记录调试日志
        log.debug("准备连接目标服务器，ip={},port={}", msg.dstAddr(), msg.dstPort());

        // 配置客户端通道，以连接到目标服务器
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(proxyGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // 添加服务端写客户端的Handler
                        ch.pipeline().addLast(new Dest2ClientInboundHandler(ctx));
                    }
                });

//        // 尝试连接目标服务器
        bootstrap.connect(msg.dstAddr(), msg.dstPort())
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        log.debug("目标服务器连接成功");
                        // 添加客户端转发请求到服务端的Handler
                        ctx.pipeline().addLast(new Client2DestInboundHandler(future));
                        // 发送成功的SOCKS5命令响应
                        DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5AddressType);
                        ctx.writeAndFlush(commandResponse);
                        // 移除不再需要的处理器
                        ctx.pipeline().remove(Socks5CommandRequestInboundHandler.class);
                        ctx.pipeline().remove(Socks5CommandRequestDecoder.class);
                    } else {
                        log.error("连接目标服务器失败,address={},port={}", msg.dstAddr(), msg.dstPort());
                        // 发送失败的SOCKS5命令响应
                        DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socks5AddressType);
                        ctx.writeAndFlush(commandResponse);
                        // 关闭连接失败的通道
                        future.channel().close();
                    }
                });
    }


}
