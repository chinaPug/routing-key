package cn.pug.routing.key.proxy.core.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 异常处理器，用于捕获和处理 Netty 管道中的业务异常。
 * <p>
 * 该类继承自 {@link io.netty.channel.ChannelDuplexHandler}，主要用于在 Netty 的处理管道中
 * 捕获未被处理的异常，并记录相关日志以便于问题排查和系统监控。
 * </p>
 *
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class ExceptionHandler extends ChannelDuplexHandler {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Pipeline 业务异常兜底触发。", cause);
    }
}
