package cn.pug.routing.key.proxy.pool.component.socks;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.ReferenceCountUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@Slf4j
@AllArgsConstructor
public class Socks5InitialRequestInboundHandler extends SimpleChannelInboundHandler<DefaultSocks5InitialRequest> {

    // 处理入站消息的方法
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5InitialRequest msg) throws Exception {
        // 记录初始化Socks5连接的日志
        log.info("初始化socks5链接");
        // 检查解码结果是否失败
        boolean failure = msg.decoderResult().isFailure();
        if (failure) {
            // 如果解码失败，记录错误日志并继续处理消息
            log.error("初始化socks5失败，请检查是否是socks5协议");
            ReferenceCountUtil.retain(msg);
            ctx.fireChannelRead(msg);
            return;
        }
        // 如果不需要认证，发送带有NO_AUTH认证方法的响应
        Socks5InitialResponse socks5InitialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH);
        ctx.writeAndFlush(socks5InitialResponse);
        // 移除当前处理器和解码器，为后续处理做准备
        ctx.pipeline().remove(this);
        ctx.pipeline().remove(Socks5InitialRequestDecoder.class);
        log.info("初始化socks5链接完成");
    }
}
