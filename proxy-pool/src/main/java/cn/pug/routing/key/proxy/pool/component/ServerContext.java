package cn.pug.routing.key.proxy.pool.component;

import cn.pug.routing.key.proxy.pool.component.config.PoolConfig;
import cn.pug.routing.key.proxy.pool.component.daemon.Daemon;
import cn.pug.routing.key.proxy.pool.component.socks.Socks5;
import com.alibaba.fastjson2.JSON;
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
    // Servlet (保持引用以防止被垃圾回收)
    private final Tomcat tomcat;
    // 端口与socks5实例的映射
    private final Map<Integer, Socks5> port2SocksProxy = new ConcurrentHashMap<>(64);

    private ServerContext() throws LifecycleException {
        daemon = new Daemon(poolConfig.proxyPort);
        tomcat = initializeTomcat();
        log.info("启动web服务成功【{}】", poolConfig.servletConfig.port);
    }
    
    /**
     * 初始化Tomcat服务器
     */
    private Tomcat initializeTomcat() throws LifecycleException {
        try {
            // 配置日志桥接
            configureLoggerBridge();
            
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(poolConfig.servletConfig.port);
            tomcat.getConnector(); // 获取默认连接器
            
            // 创建上下文
            String contextPath = poolConfig.servletConfig.path.startsWith("/") 
                ? poolConfig.servletConfig.path 
                : "/" + poolConfig.servletConfig.path;
            Context context = tomcat.addContext(contextPath, new File(".").getAbsolutePath());

            // 添加状态查询Servlet
            addAvailablePortsServlet(tomcat, contextPath);
            context.addServletMappingDecoded("/*", "available");
            
            tomcat.start();
            return tomcat;
        } catch (Exception e) {
            log.error("初始化Tomcat失败", e);
            throw new LifecycleException("Failed to initialize Tomcat", e);
        }
    }
    
    /**
     * 配置日志桥接，将JUL日志桥接到SLF4J
     */
    private void configureLoggerBridge() {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
    
    /**
     * 添加可用端口查询Servlet
     */
    private void addAvailablePortsServlet(Tomcat tomcat, String contextPath) {
        tomcat.addServlet(contextPath, "available", new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("application/json;charset=UTF-8");
                resp.setHeader("Cache-Control", "no-cache");
                
                try {
                    Set<Integer> availablePorts = port2SocksProxy.keySet();
                    log.debug("查询可用代理端口：【{}】", availablePorts);
                    
                    String jsonResponse = JSON.toJSONString(availablePorts);
                    resp.getWriter().write(jsonResponse);
                    resp.setStatus(HttpServletResponse.SC_OK);
                } catch (Exception e) {
                    log.error("获取可用端口失败", e);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{\"error\":\"Internal server error\"}");
                }
            }
        });
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
        log.info("开始关闭ServerContext...");
        
        // 关闭所有代理
        for (Socks5 socks5 : port2SocksProxy.values()) {
            try {
                socks5.shutdownGracefully();
            } catch (Exception e) {
                log.warn("关闭代理时发生异常", e);
            }
        }
        port2SocksProxy.clear();
        
        // 关闭守护进程
        try {
            daemon.shutdownGracefully();
        } catch (Exception e) {
            log.warn("关闭守护进程时发生异常", e);
        }
        
        // 关闭Tomcat
        try {
            if (tomcat != null) {
                tomcat.stop();
                tomcat.destroy();
                log.info("Tomcat服务已停止");
            }
        } catch (Exception e) {
            log.warn("关闭Tomcat时发生异常", e);
        }
        
        log.info("ServerContext已关闭");
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
