package cn.pug.routing.key.proxy.pool.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 代理池监控指标
 * 
 * 集成 Prometheus，提供连接数、流量、延迟等指标
 * 
 * @author pug
 * @since 1.0.0
 */
@Slf4j
@Getter
public class ProxyMetrics {

    private final MeterRegistry registry;
    
    // 连接指标
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger registeredUnits = new AtomicInteger(0);
    
    // 计数器
    private final Counter totalConnections;
    private final Counter failedConnections;
    private final Counter bytesTransferred;
    
    // 计时器
    private final Timer connectionDuration;
    private final Timer routingLatency;

    public ProxyMetrics() {
        this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        
        // 注册 Gauge（当前值）
        Gauge.builder("proxy.active.connections", activeConnections, AtomicInteger::get)
                .description("当前活跃连接数")
                .register(registry);
        
        Gauge.builder("proxy.registered.units", registeredUnits, AtomicInteger::get)
                .description("已注册的代理节点数")
                .register(registry);
        
        // 注册 Counter（累计值）
        this.totalConnections = Counter.builder("proxy.connections.total")
                .description("总连接数")
                .register(registry);
        
        this.failedConnections = Counter.builder("proxy.connections.failed")
                .description("失败连接数")
                .register(registry);
        
        this.bytesTransferred = Counter.builder("proxy.bytes.transferred")
                .description("传输字节数")
                .baseUnit("bytes")
                .register(registry);
        
        // 注册 Timer（耗时）
        this.connectionDuration = Timer.builder("proxy.connection.duration")
                .description("连接持续时间")
                .register(registry);
        
        this.routingLatency = Timer.builder("proxy.routing.latency")
                .description("路由延迟")
                .register(registry);
        
        log.info("Prometheus 监控指标已初始化");
    }

    /**
     * 记录新连接
     */
    public void recordNewConnection() {
        activeConnections.incrementAndGet();
        totalConnections.increment();
    }

    /**
     * 记录连接关闭
     */
    public void recordConnectionClosed() {
        activeConnections.decrementAndGet();
    }

    /**
     * 记录连接失败
     */
    public void recordConnectionFailed() {
        failedConnections.increment();
    }

    /**
     * 记录代理节点注册
     */
    public void recordUnitRegistered() {
        registeredUnits.incrementAndGet();
    }

    /**
     * 记录代理节点注销
     */
    public void recordUnitUnregistered() {
        registeredUnits.decrementAndGet();
    }

    /**
     * 记录数据传输
     */
    public void recordBytesTransferred(long bytes) {
        bytesTransferred.increment(bytes);
    }

    /**
     * 记录路由延迟
     */
    public void recordRoutingLatency(long millis) {
        routingLatency.record(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取 Prometheus 指标数据
     */
    public String scrape() {
        return ((PrometheusMeterRegistry) registry).scrape();
    }
}
