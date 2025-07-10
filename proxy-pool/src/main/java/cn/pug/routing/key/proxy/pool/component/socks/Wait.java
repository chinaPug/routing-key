package cn.pug.routing.key.proxy.pool.component.socks;

import cn.pug.common.protocol.RoutingKeyProtocol;
import cn.pug.routing.key.proxy.pool.component.inboundHandler.Browser2ServerInboundHandler;
import cn.pug.routing.key.proxy.pool.component.inboundHandler.Client2ServerInboundHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Wait extends SimpleChannelInboundHandler<String> {

    private Socks5 socks5;
    public Wait(Socks5 socks5) {
        this.socks5 = socks5;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        log.info(msg);
        if (msg.contains(RoutingKeyProtocol.SEGMENT_SPLIT+"false")){
            log.error("连接失败");
            ChannelHandlerContext toBrowserCtx = socks5.des2toBrowserCtxMap.get(msg.replace(RoutingKeyProtocol.SEGMENT_SPLIT+"false",""));

            DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socks5.des2toSocks5AddressTypeMap.remove(msg.replace(RoutingKeyProtocol.SEGMENT_SPLIT+"false","")));
            toBrowserCtx.writeAndFlush(commandResponse);
        }else {
            ChannelHandlerContext toBrowserCtx = socks5.des2toBrowserCtxMap.remove(msg);
            ctx.pipeline().remove(DelimiterBasedFrameDecoder.class);
            ctx.pipeline().remove(StringDecoder.class);
            ctx.pipeline().remove(StringEncoder.class);
            ctx.pipeline().remove(this);
            ctx.pipeline().addLast(new Client2ServerInboundHandler(toBrowserCtx));
            toBrowserCtx.pipeline().addLast(new Browser2ServerInboundHandler(ctx));
            log.info("成功建立浏览器和代理服务器之间的通道");
            //转发成功
            DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5.des2toSocks5AddressTypeMap.remove(msg));
            toBrowserCtx.writeAndFlush(commandResponse);
            if (toBrowserCtx.pipeline().get(Socks5CommandRequestInboundHandler.class) != null)
                toBrowserCtx.pipeline().remove(Socks5CommandRequestInboundHandler.class);
            if (toBrowserCtx.pipeline().get(Socks5CommandRequestDecoder.class) != null)
                toBrowserCtx.pipeline().remove(Socks5CommandRequestDecoder.class);
        }
    }

}
