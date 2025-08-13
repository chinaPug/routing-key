package cn.pug.routing.key.proxy.core.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
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

    private final Channel targetChannel;

    public P2PInboundHandler(Channel targetChannel) {
        if (targetChannel == null) {
            throw new IllegalArgumentException("Target channel cannot be null");
        }
        this.targetChannel = targetChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (targetChannel.isActive()) {
            targetChannel.writeAndFlush(msg).addListener(future -> {
                if (!future.isSuccess()) {
                    log.warn("转发消息失败: {}", future.cause().getMessage());
                    ReferenceCountUtil.release(msg); // 释放消息引用
                    ctx.close();
                }
            });
        } else {
            log.debug("目标通道已关闭，释放消息");
            ReferenceCountUtil.release(msg);
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("源通道关闭，同时关闭目标通道");
        if (targetChannel.isActive()) {
            targetChannel.close();
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("P2P连接异常: {}", cause.getMessage());
        ctx.close();
        if (targetChannel.isActive()) {
            targetChannel.close();
        }
    }
}
