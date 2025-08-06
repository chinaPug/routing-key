package cn.pug.routing.key.proxy.pool.component.socks;

import cn.pug.common.protocol.RoutingKeyDecoder;
import cn.pug.common.protocol.RoutingKeyProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class SplitFlowInboundHandler extends ByteToMessageDecoder {
    Socks5 socks5;

    public SplitFlowInboundHandler(Socks5 socks5){
        this.socks5=socks5;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        log.info("分流解析器");
        // 只需要读一个字节，即读取魔数，就可以做到分流效果
        if (in.readableBytes()<1){
            return;
        }
        // 取出pipeline
        final ChannelPipeline pipeline = ctx.pipeline();
        pipeline.remove(this);
        // 标记初始读取位置，分流后需要置回
        in.markReaderIndex();
        int magicNumber = in.readUnsignedByte();
        // 是routing-key协议规范
        if (magicNumber== RoutingKeyProtocol.MAGIC_NUMBER){
            log.info("分流到routing-key协议");
            pipeline
                    .addLast(new RoutingKeyDecoder())
                    // 增加等待建立处理器
                    .addLast(new RoutingResponseInboundHandler(socks5));
//                    .addLast(new ExceptionHandler());
        }else {
            log.info("分流到socks5协议");
            pipeline// 添加Socks5编码器
                    .addLast(Socks5ServerEncoder.DEFAULT)
                    // 处理Socks5初始化请求
                    .addLast(new Socks5InitialRequestDecoder())
                    .addLast(new Socks5InitialRequestInboundHandler())
                    // 处理连接请求
                    .addLast(new Socks5CommandRequestDecoder())
                    .addLast(new Socks5CommandRequestInboundHandler(socks5));
//                    .addLast(new ExceptionHandler());
        }
        in.resetReaderIndex();
        pipeline.fireChannelRead(in.retain());
    }
}
