package cn.pug.server.component.socks;

import cn.pug.server.component.inboundHandler.Browser2ServerInboundHandler;
import cn.pug.server.component.inboundHandler.Client2ServerInboundHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
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
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        log.info(msg);
        ChannelHandlerContext toBrowserCtx = socks5.des2toBrowserCtxMap.get(msg);
        if (toBrowserCtx==null){
            log.error("没有找到对应的浏览器通道");
        }else {
            ctx.pipeline().remove(DelimiterBasedFrameDecoder.class);
            ctx.pipeline().remove(StringDecoder.class);
            ctx.pipeline().remove(StringEncoder.class);
            ctx.pipeline().remove(this);
            ctx.pipeline().addLast(new Browser2ServerInboundHandler(ctx))
                    .addLast(new Client2ServerInboundHandler(toBrowserCtx));
            log.info("成功建立浏览器和代理服务器之间的通道");
            //转发成功
            DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5.des2toSocks5AddressTypeMap.get(msg));
            toBrowserCtx.writeAndFlush(commandResponse);
            toBrowserCtx.pipeline().remove(Socks5CommandRequestDecoder.class);
        }
    }
}
