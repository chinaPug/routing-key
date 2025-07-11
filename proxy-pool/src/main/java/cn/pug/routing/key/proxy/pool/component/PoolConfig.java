package cn.pug.routing.key.proxy.pool.component;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class PoolConfig {
    String ip;
    int proxyPort;
    ServletConfig servletConfig;

    {
        Yaml yaml = new Yaml();
        try (InputStream in = PoolConfig.class.getResourceAsStream("/routing-key.yml")) {
            Map<String, Object> config = yaml.load(in);
            ip = (String) config.getOrDefault("proxy.ip", "127.0.0.1");
            proxyPort = (int) config.getOrDefault("proxy.port", 8080);
            servletConfig = new ServletConfig();
            servletConfig.port = (int) config.getOrDefault("servlet.port", 8085);
            servletConfig.path = (String) config.getOrDefault("servlet.path", "/available");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    
    public enum PoolConfigHolder {
        INSTANCE;
        private final PoolConfig poolConfig;

        PoolConfigHolder() {
            poolConfig = new PoolConfig();
        }

        public PoolConfig getPoolConfig() {
            return poolConfig;
        }
    }

    public static class ServletConfig {
        int port;
        String path;
    }
}
