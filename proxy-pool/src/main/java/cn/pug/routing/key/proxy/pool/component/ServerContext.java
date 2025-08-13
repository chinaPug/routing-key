package cn.pug.routing.key.proxy.pool.component;

import cn.pug.routing.key.proxy.pool.component.config.PoolConfig;
import cn.pug.routing.key.proxy.pool.component.daemon.Daemon;
import cn.pug.routing.key.proxy.pool.component.socks.Socks5;
import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.LogManager;

@Slf4j
public class ServerContext {
    //配置类
    private final PoolConfig poolConfig = PoolConfig.PoolConfigHolder.INSTANCE.getPoolConfig();
    // 守护进程
    private final Daemon daemon;
    // Servlet
    private final Tomcat tomcat;
    // 端口与socks5实例的映射
    private final Map<Integer, Socks5> port2SocksProxy = new ConcurrentHashMap<>(64);

    private ServerContext() throws LifecycleException {
        daemon = new Daemon(poolConfig.proxyPort);
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        tomcat = new Tomcat();
        tomcat.setPort(poolConfig.servletConfig.port);
        tomcat.getConnector();
        Context context=tomcat.addContext(poolConfig.servletConfig.path,new File(".").getAbsolutePath());

        // 添加 Servlet 到已配置的 Context 中
        tomcat.addServlet(poolConfig.servletConfig.path,"available" , new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                // 设置响应类型为 JSON
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                // 将 可用端口 转换为 JSON 字符串并写入响应
                log.info("可用代理端口：【{}】", port2SocksProxy.keySet());
                String jsonResponse = JSON.toJSONString(port2SocksProxy.keySet());
                resp.getWriter().write(jsonResponse);
            }
        });
        context.addServletMapping("/*","available");
        tomcat.start();
        log.info("启动web服务成功【{}】", poolConfig.servletConfig.port);
    }


    // 启动服务
    public ServerContext start() {
        log.info("启动proxy-pool服务");
        daemon.start();
        return ServerContext.this;
    }

    // 注册代理
    public void registrySocksProxy(int port, Socks5 socks5) {
        port2SocksProxy.put(port, socks5);
    }

    // 移除代理
    public void removeSocksProxy(int port) {
        port2SocksProxy.remove(port);
    }

    // 暴露代理
    public Set<Integer> getAvailableProxy() {
        return port2SocksProxy.keySet();
    }

    // 停止所有代理
    public void shutdownGracefully() {
        for (Socks5 socks5 : port2SocksProxy.values()) {
            socks5.shutdownGracefully();
        }
    }

    @Getter
    public enum ServerContextHolder {
        INSTANCE;
        private final ServerContext serverContext;

        ServerContextHolder() {
            try {
                serverContext = new ServerContext();
            } catch (LifecycleException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
