package cn.pug.routing.key.proxy.core.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳检测处理器
 * 
 * 用于检测连接是否存活，防止僵尸连接
 * 
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    private final String handlerName;
    private int heartbeatMissCount = 0;
    private static final int MAX_MISSED_HEARTBEATS = 3;

    public HeartbeatHandler(String handlerName) {
        this.handlerName = handlerName;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                heartbeatMissCount++;
                log.warn("[{}] 心跳检测失败，第 {} 次", handlerName, heartbeatMissCount);
                
                if (heartbeatMissCount >= MAX_MISSED_HEARTBEATS) {
                    log.error("[{}] 心跳检测失败超过 {} 次，关闭连接", handlerName, MAX_MISSED_HEARTBEATS);
                    ctx.close();
                }
            } else if (event.state() == IdleState.WRITER_IDLE) {
                // 可以在这里发送心跳包
                log.debug("[{}] 发送心跳包", handlerName);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 收到任何消息都重置心跳计数
        heartbeatMissCount = 0;
        super.channelRead(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("[{}] 心跳处理器异常: {}", handlerName, cause.getMessage());
        ctx.close();
    }
}
