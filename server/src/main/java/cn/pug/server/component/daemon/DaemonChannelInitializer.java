package cn.pug.server.component.daemon;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class DaemonChannelInitializer extends ChannelInitializer<SocketChannel> {
    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // 1 该服务为daemon服务，在该业务逻辑单元会处理以下几件事：
        // 1.1 开放一个tcp端口，接收客户端连接，相当于服务注册请求的处理；todo 报文需要定义
        // 1.2 接收到服务注册报文后，需要在代理池约定的端口范围内，找到一个可用的端口并创建一个socks服务，准备接收连接
        // 1.3 将创建的socks服务的端口号发送给客户端，并保存该端口号与客户端之间的映射关系，用于后续的连接转发
        // 到此为止，该daemon服务的任务完成，总结：负责处理客户端代理服务注册和分配本地代理服务相关信息
        ch.pipeline()
                .addLast(new StringDecoder())
                .addLast(new StringEncoder())
                .addLast(new TcpProxyRegistryHandler(9000,10000));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("代理守护服务异常",cause);
    }
}