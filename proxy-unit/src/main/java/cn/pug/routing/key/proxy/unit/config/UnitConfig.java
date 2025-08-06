package cn.pug.routing.key.proxy.unit.config;

import lombok.Getter;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

@Getter
public class UnitConfig {
    private final String hostname;
    private final ProxyConfig proxyConfig;
    {
        Yaml yaml = new Yaml();
        try (InputStream in = UnitConfig.class.getResourceAsStream("/routing-key.yml")) {
            Map<String, Object> config = yaml.load(in);
            hostname = (String) config.getOrDefault("unit-hostname", "myself");
            proxyConfig = new ProxyConfig();
            proxyConfig.ip = (String) config.getOrDefault("proxy-ip", "1.94.56.249");
            proxyConfig.port = (int) config.getOrDefault("proxy-port", 8080);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        private String ip;
        private int port;
    }

}
