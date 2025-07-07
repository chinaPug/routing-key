package cn.pug.client.component.init;

import cn.pug.common.utils.NetUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Init {
    private EventLoopGroup bossGroup=new NioEventLoopGroup();
    private EventLoopGroup proxyGroup=new NioEventLoopGroup();
    private Bootstrap bootstrap=new Bootstrap();
    private String host;
    private int port;
    public Init(String host,int port){
        this.host=host;
        this.port=port;
    }
    public void start(){
        try {
            this.bootstrap
                    .group(this.bossGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new StringDecoder())
                                    .addLast(new StringEncoder())
                                    // 发送注册服务报文
                                    .addLast(new TcpInitialRequestInboundHandler(proxyGroup));
                        }
                    });
            Channel channel = this.bootstrap.connect(this.host,this.port).sync().channel();
            // 发送注册信息
            channel.writeAndFlush(NetUtil.getLocalIP()).addListener(
                    future -> {
                        if (future.isSuccess()) {
                            log.info("注册服务成功!");
                        }else {
                            log.error("注册服务失败");
                        }
                    }
            );
            // 阻塞等待关闭
            channel.closeFuture().sync();

        }catch (Exception e){
            log.error("客户端初始化服务启动异常",e);
        }finally {
            this.bossGroup.shutdownGracefully();
            this.proxyGroup.shutdownGracefully();
        }
    }
}
