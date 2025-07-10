package cn.pug.server.component;

import cn.pug.server.component.daemon.Daemon;
import cn.pug.server.component.socks.Socks5;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ServerContext {
    private static ServerContext instance;
    private final Daemon daemon;
    private final Map<Integer,Socks5> port2SocksProxy = new ConcurrentHashMap<>(64);
    public ServerContext(int port) {
        daemon = new Daemon(port);
        if (ServerContext.instance !=null) {
            log.error("ServerContext实例已存在，服务中仅可存在一个ServerContext实例！");
            throw new RuntimeException("ServerContext实例已存在，服务中仅可存在一个ServerContext实例！");
        }
        ServerContext.instance = this;
    }

    public static ServerContext getInstance(){
        return ServerContext.instance;
    }

    public void start() {
        daemon.start();
    }

    public void registrySocksProxy(int port, Socks5 socks5) {
        port2SocksProxy.put(port,socks5);
    }

    public void removeSocksProxy(int port) {
         port2SocksProxy.remove(port);
    }


    public void shutdownGracefully() {
        for (Socks5 socks5 : port2SocksProxy.values()) {
            socks5.shutdownGracefully();
        }
    }
}
