package cn.pug.routing.key.proxy.unit.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class UnitConfig {
    private final String hostname;
    private final ProxyConfig proxyConfig;
    
    public UnitConfig() {
        Map<String, Object> config = loadConfiguration();
        this.hostname = getStringValue(config, "unit-hostname", "myself");
        this.proxyConfig = new ProxyConfig(
            getStringValue(config, "proxy-ip", "127.0.0.1"),
            getIntValue(config, "proxy-port", 8080)
        );
    }
    
    private Map<String, Object> loadConfiguration() {
        Yaml yaml = new Yaml();
        try (InputStream in = UnitConfig.class.getResourceAsStream("/routing-key.yml")) {
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
    public enum UnitConfigHolder {
        INSTANCE;
        private final UnitConfig unitConfig;

        UnitConfigHolder() {
            unitConfig = new UnitConfig();
        }

    }
    @Getter
    public static class ProxyConfig{
        private final String ip;
        private final int port;
        
        public ProxyConfig(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }

}
