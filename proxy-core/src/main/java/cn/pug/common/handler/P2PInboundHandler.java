package cn.pug.common.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * P2PInboundHandler 是一个用于处理点对点通信的 Netty 入站处理器。
 * <p>
 * 该处理器将接收到的消息转发到指定的通道，并在连接断开时关闭通道。
 * </p>
 *
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class P2PInboundHandler extends ChannelInboundHandlerAdapter {

    private final Channel channel;

    public P2PInboundHandler(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        channel.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channel.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

    }
}
