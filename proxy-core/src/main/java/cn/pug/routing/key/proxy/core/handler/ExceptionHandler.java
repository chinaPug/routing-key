package cn.pug.routing.key.proxy.core.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;

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
        if (isIgnorableException(cause)) {
            log.debug("忽略的网络异常: {}", cause.getMessage());
        } else {
            log.error("Pipeline 业务异常，远程地址: {}", ctx.channel().remoteAddress(), cause);
        }
        
        // 确保连接被正确关闭
        if (ctx.channel().isActive()) {
            ctx.close();
        }
    }
    
    /**
     * 判断是否为可忽略的异常（如连接重置等正常网络异常）
     */
    private boolean isIgnorableException(Throwable cause) {
        if (cause instanceof IOException) {
            String message = cause.getMessage();
            if (message != null) {
                return message.contains("Connection reset") 
                    || message.contains("Broken pipe")
                    || message.contains("Connection aborted");
            }
        }
        return cause instanceof SocketException 
            || cause instanceof ClosedChannelException;
    }
}
