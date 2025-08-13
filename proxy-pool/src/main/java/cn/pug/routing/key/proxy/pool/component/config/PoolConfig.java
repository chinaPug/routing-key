package cn.pug.routing.key.proxy.pool.component.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PoolConfig {
    public final int proxyPort;
    public final ServletConfig servletConfig;

    public PoolConfig() {
        Map<String, Object> config = loadConfiguration();
        this.proxyPort = getIntValue(config, "proxy-port", 8080);
        this.servletConfig = new ServletConfig(
            getIntValue(config, "servlet-port", 8085),
            getStringValue(config, "servlet-path", "available")
        );
    }
    
    private Map<String, Object> loadConfiguration() {
        Yaml yaml = new Yaml();
        try (InputStream in = PoolConfig.class.getResourceAsStream("/routing-key.yml")) {
            if (in == null) {
                log.warn("配置文件 routing-key.yml 不存在，使用默认配置");
                return new HashMap<>();
            }
            Map<String, Object> config = yaml.load(in);
            return config != null ? config : new HashMap<>();
        } catch (Exception e) {
            log.error("加载配置文件失败，使用默认配置", e);
            return new HashMap<>();
        }
    }
    
    private int getIntValue(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return defaultValue;
    }
    
    private String getStringValue(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    
    @Getter
    public enum PoolConfigHolder {
        INSTANCE;
        private final PoolConfig poolConfig;

        PoolConfigHolder() {
            poolConfig = new PoolConfig();
        }

    }

    public static class ServletConfig {
        public final int port;
        public final String path;
        
        public ServletConfig(int port, String path) {
            this.port = port;
            this.path = path.startsWith("/") ? path : "/" + path;
        }
    }
}
