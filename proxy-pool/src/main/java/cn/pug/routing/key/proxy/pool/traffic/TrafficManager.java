package cn.pug.routing.key.proxy.pool.traffic;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 流量统计和限流管理器
 * 
 * 统计流量、限制速率，防止过载
 * 
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class TrafficManager {

    private final AtomicLong totalBytesIn = new AtomicLong(0);
    private final AtomicLong totalBytesOut = new AtomicLong(0);
    private final AtomicLong totalConnections = new AtomicLong(0);
    
    private final ConcurrentHashMap<String, TrafficStats> unitTrafficMap = new ConcurrentHashMap<>();
    
    // 限流配置
    private volatile long maxBytesPerSecond = 10 * 1024 * 1024; // 默认10MB/s
    private volatile int maxConnectionsPerUnit = 100;
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Traffic-Manager");
        t.setDaemon(true);
        return t;
    });

    public TrafficManager() {
        // 启动流量重置定时器（每秒重置速率计数）
        scheduler.scheduleAtFixedRate(this::resetRateCounters, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 记录入站流量
     */
    public void recordBytesIn(String unitId, long bytes) {
        totalBytesIn.addAndGet(bytes);
        getUnitStats(unitId).addBytesIn(bytes);
    }

    /**
     * 记录出站流量
     */
    public void recordBytesOut(String unitId, long bytes) {
        totalBytesOut.addAndGet(bytes);
        getUnitStats(unitId).addBytesOut(bytes);
    }

    /**
     * 记录新连接
     */
    public boolean recordNewConnection(String unitId) {
        TrafficStats stats = getUnitStats(unitId);
        
        // 检查连接数限制
        if (stats.getCurrentConnections() >= maxConnectionsPerUnit) {
            log.warn("节点 [{}] 连接数超过限制: {}/{}", 
                unitId, stats.getCurrentConnections(), maxConnectionsPerUnit);
            return false;
        }
        
        stats.incrementConnections();
        totalConnections.incrementAndGet();
        return true;
    }

    /**
     * 记录连接关闭
     */
    public void recordConnectionClosed(String unitId) {
        getUnitStats(unitId).decrementConnections();
        totalConnections.decrementAndGet();
    }

    /**
     * 检查是否超过速率限制
     */
    public boolean isRateLimited(String unitId) {
        TrafficStats stats = getUnitStats(unitId);
        long currentRate = stats.getBytesInPerSecond() + stats.getBytesOutPerSecond();
        
        if (currentRate > maxBytesPerSecond) {
            log.warn("节点 [{}] 流量超过限制: {}/{} bytes/s", 
                unitId, currentRate, maxBytesPerSecond);
            return true;
        }
        return false;
    }

    /**
     * 获取或创建节点统计
     */
    private TrafficStats getUnitStats(String unitId) {
        return unitTrafficMap.computeIfAbsent(unitId, k -> new TrafficStats(k));
    }

    /**
     * 重置速率计数器（每秒调用）
     */
    private void resetRateCounters() {
        for (TrafficStats stats : unitTrafficMap.values()) {
            stats.resetRateCounters();
        }
    }

    /**
     * 获取总统计
     */
    public GlobalStats getGlobalStats() {
        return new GlobalStats(
            totalBytesIn.get(),
            totalBytesOut.get(),
            totalConnections.get()
        );
    }

    /**
     * 获取节点统计
     */
    public TrafficStats getUnitStats(String unitId) {
        return unitTrafficMap.get(unitId);
    }

    // Getters and Setters
    public void setMaxBytesPerSecond(long maxBytesPerSecond) {
        this.maxBytesPerSecond = maxBytesPerSecond;
    }

    public void setMaxConnectionsPerUnit(int maxConnectionsPerUnit) {
        this.maxConnectionsPerUnit = maxConnectionsPerUnit;
    }

    @Data
    public static class TrafficStats {
        private final String unitId;
        private final AtomicLong bytesInTotal = new AtomicLong(0);
        private final AtomicLong bytesOutTotal = new AtomicLong(0);
        private final AtomicLong bytesInPerSecond = new AtomicLong(0);
        private final AtomicLong bytesOutPerSecond = new AtomicLong(0);
        private final AtomicLong currentConnections = new AtomicLong(0);

        public TrafficStats(String unitId) {
            this.unitId = unitId;
        }

        public void addBytesIn(long bytes) {
            bytesInTotal.addAndGet(bytes);
            bytesInPerSecond.addAndGet(bytes);
        }

        public void addBytesOut(long bytes) {
            bytesOutTotal.addAndGet(bytes);
            bytesOutPerSecond.addAndGet(bytes);
        }

        public void incrementConnections() {
            currentConnections.incrementAndGet();
        }

        public void decrementConnections() {
            currentConnections.decrementAndGet();
        }

        public void resetRateCounters() {
            bytesInPerSecond.set(0);
            bytesOutPerSecond.set(0);
        }
    }

    @Data
    public static class GlobalStats {
        private final long totalBytesIn;
        private final long totalBytesOut;
        private final long totalConnections;

        public double getTotalMBIn() {
            return totalBytesIn / (1024.0 * 1024.0);
        }

        public double getTotalMBOut() {
            return totalBytesOut / (1024.0 * 1024.0);
        }
    }
}
