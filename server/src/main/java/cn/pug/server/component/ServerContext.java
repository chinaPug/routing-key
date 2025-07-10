package cn.pug.server.component;

import cn.pug.server.component.daemon.Daemon;
import cn.pug.server.component.socks.Socks5;
import com.google.common.collect.HashBiMap;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ServerContext {
    // 单例，一个服务中仅可存在一个ServerContext实例
    private static ServerContext instance;
    private String ip;
    // 守护进程
    private final Daemon daemon;
    // 端口与socks5实例的映射
    private final Map<Integer, Socks5> port2SocksProxy = new ConcurrentHashMap<>(64);

    // 构造函数：传入守护进程的port
    public ServerContext(String ip,int port) {
        this.ip= ip;
        daemon = new Daemon(port);
        if (ServerContext.instance != null) {
            log.error("ServerContext实例已存在，服务中仅可存在一个ServerContext实例！");
            throw new RuntimeException("ServerContext实例已存在，服务中仅可存在一个ServerContext实例！");
        }
        ServerContext.instance = this;
    }

    // 获取单例方法
    public static ServerContext getInstance() {
        return ServerContext.instance;
    }

    // 启动服务
    public void start() {
        daemon.start();
    }

    // 注册代理
    public void registrySocksProxy(int port, Socks5 socks5) {
        port2SocksProxy.put(port, socks5);
    }
    // 移除代理
    public void removeSocksProxy(int port) {
        port2SocksProxy.remove(port);
    }

    // 停止所有代理
    public void shutdownGracefully() {
        for (Socks5 socks5 : port2SocksProxy.values()) {
            socks5.shutdownGracefully();
        }
    }

    public String getIp() {
        return ip;
    }
}
