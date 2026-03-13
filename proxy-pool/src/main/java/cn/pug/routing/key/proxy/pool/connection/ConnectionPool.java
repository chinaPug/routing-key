package cn.pug.routing.key.proxy.pool.connection;

import io.netty.channel.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 连接池管理器
 * 
 * 优化连接复用，减少资源消耗
 * 
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class ConnectionPool {

    private final String poolName;
    private final int maxConnections;
    private final long connectionTimeoutMs;
    private final long idleTimeoutMs;

    private final BlockingQueue<PooledConnection> availableConnections;
    private final ConcurrentMap<Channel, PooledConnection> activeConnections = new ConcurrentHashMap<>();
    private final AtomicInteger totalConnections = new AtomicInteger(0);

    public ConnectionPool(String poolName, int maxConnections, long connectionTimeoutMs, long idleTimeoutMs) {
        this.poolName = poolName;
        this.maxConnections = maxConnections;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.idleTimeoutMs = idleTimeoutMs;
        this.availableConnections = new LinkedBlockingQueue<>(maxConnections);

        // 启动空闲连接清理线程
        startIdleConnectionCleaner();
    }

    /**
     * 获取连接
     */
    public PooledConnection acquire(Channel channel) throws InterruptedException {
        // 先尝试获取可用连接
        PooledConnection conn = availableConnections.poll();

        if (conn != null) {
            conn.setChannel(channel);
            conn.setLastUsedTime(System.currentTimeMillis());
            activeConnections.put(channel, conn);
            log.debug("[{}] 复用连接，当前活跃: {}", poolName, activeConnections.size());
            return conn;
        }

        // 如果没有可用连接，且未达到上限，创建新连接
        if (totalConnections.get() < maxConnections) {
            conn = createNewConnection(channel);
            activeConnections.put(channel, conn);
            log.debug("[{}] 创建新连接，总数: {}", poolName, totalConnections.incrementAndGet());
            return conn;
        }

        // 等待可用连接
        conn = availableConnections.poll(connectionTimeoutMs, TimeUnit.MILLISECONDS);
        if (conn != null) {
            conn.setChannel(channel);
            conn.setLastUsedTime(System.currentTimeMillis());
            activeConnections.put(channel, conn);
            return conn;
        }

        throw new ConnectionPoolExhaustedException("连接池耗尽: " + poolName);
    }

    /**
     * 释放连接
     */
    public void release(Channel channel) {
        PooledConnection conn = activeConnections.remove(channel);
        if (conn != null) {
            conn.setChannel(null);
            conn.setLastUsedTime(System.currentTimeMillis());

            // 如果连接有效，放回池中
            if (conn.isValid()) {
                availableConnections.offer(conn);
                log.debug("[{}] 释放连接回池，可用: {}", poolName, availableConnections.size());
            } else {
                totalConnections.decrementAndGet();
                log.debug("[{}] 关闭无效连接，总数: {}", poolName, totalConnections.get());
            }
        }
    }

    /**
     * 创建新连接
     */
    private PooledConnection createNewConnection(Channel channel) {
        PooledConnection conn = new PooledConnection();
        conn.setChannel(channel);
        conn.setCreatedTime(System.currentTimeMillis());
        conn.setLastUsedTime(System.currentTimeMillis());
        conn.setValid(true);
        return conn;
    }

    /**
     * 启动空闲连接清理线程
     */
    private void startIdleConnectionCleaner() {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ConnectionPool-Cleaner-" + poolName);
            t.setDaemon(true);
            return t;
        });

        cleaner.scheduleAtFixedRate(() -> {
            try {
                cleanupIdleConnections();
            } catch (Exception e) {
                log.error("清理空闲连接失败: {}", e.getMessage());
            }
        }, idleTimeoutMs, idleTimeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 清理空闲连接
     */
    private void cleanupIdleConnections() {
        long now = System.currentTimeMillis();
        int cleaned = 0;

        for (PooledConnection conn : availableConnections) {
            if (now - conn.getLastUsedTime() > idleTimeoutMs) {
                if (availableConnections.remove(conn)) {
                    conn.setValid(false);
                    totalConnections.decrementAndGet();
                    cleaned++;
                }
            }
        }

        if (cleaned > 0) {
            log.info("[{}] 清理 {} 个空闲连接，当前总数: {}", poolName, cleaned, totalConnections.get());
        }
    }

    /**
     * 获取统计信息
     */
    public PoolStats getStats() {
        return new PoolStats(
                totalConnections.get(),
                activeConnections.size(),
                availableConnections.size(),
                maxConnections
        );
    }

    @Data
    public static class PooledConnection {
        private Channel channel;
        private long createdTime;
        private long lastUsedTime;
        private volatile boolean valid;
    }

    @Data
    public static class PoolStats {
        private final int total;
        private final int active;
        private final int available;
        private final int max;
    }

    public static class ConnectionPoolExhaustedException extends RuntimeException {
        public ConnectionPoolExhaustedException(String message) {
            super(message);
        }
    }
}
