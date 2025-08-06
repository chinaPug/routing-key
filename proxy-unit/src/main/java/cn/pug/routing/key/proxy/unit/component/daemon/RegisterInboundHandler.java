package cn.pug.routing.key.proxy.unit.component.daemon;

import cn.pug.common.protocol.RegisterRequestEncoder;
import cn.pug.common.protocol.parser.RegisterParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegisterInboundHandler extends SimpleChannelInboundHandler<RegisterParser.RegisterPacket> {
    private Daemon daemon;
    public RegisterInboundHandler(Daemon daemon) {
        this.daemon=daemon;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RegisterParser.RegisterPacket registerPacket) throws Exception {
        log.info("注册成功，proxy pool的端口为【{}】",registerPacket.socksPort);
        // 设定socksPort
        daemon.socksPort=registerPacket.socksPort;
        ChannelPipeline pipeline = ctx.pipeline();
        // 移除本注册处理器
        pipeline.remove(this);
        // 添加路由处理器
        pipeline.addLast(new RoutingInboundHandler(daemon));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 发送注册信息
        ctx.writeAndFlush(RegisterRequestEncoder.MSG).addListener(
                future -> {
                    if (future.isSuccess()) {
                        log.info("发送注册请求成功");
                    } else {
                        log.error("发送注册请求失败");
                    }
                }
        );
    }
}
