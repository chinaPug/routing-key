package cn.pug.routing.key.proxy.unit.component.daemon;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class UnitConfig {
    String hostname;
    ProxyConfig proxyConfig;
    {
        Yaml yaml = new Yaml();
        try (InputStream in = UnitConfig.class.getResourceAsStream("/routing-key.yml")) {
            Map<String, Object> config = yaml.load(in);
            hostname = (String) config.getOrDefault("unit.hostname", "myself");
            proxyConfig = new ProxyConfig();
            proxyConfig.ip = (String) config.getOrDefault("proxy.ip", "localhost");
            proxyConfig.port = (int) config.getOrDefault("proxy.port", 8080);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    
    public enum UnitConfigHolder {
        INSTANCE;
        private final UnitConfig unitConfig;

        UnitConfigHolder() {
            unitConfig = new UnitConfig();
        }

        public UnitConfig getUnitConfig() {
            return unitConfig;
        }
    }

    public static class ProxyConfig{
        String ip;
        int port;
    }

}
