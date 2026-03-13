package cn.pug.routing.key.proxy.pool.component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 代理节点实例信息
 * 
 * 用于负载均衡和监控
 * 
 * @author pug
 * @since 1.0.0
 */
@Data
@Slf4j
public class UnitInstance {
    
    private String unitId;
    private String hostname;
    private int socksPort;
    private volatile boolean active = true;
    private volatile int connectionCount = 0;
    private volatile int weight = 1;
    private long registerTime;
    private long lastHeartbeatTime;

    public UnitInstance(String unitId, String hostname, int socksPort) {
        this.unitId = unitId;
        this.hostname = hostname;
        this.socksPort = socksPort;
        this.registerTime = System.currentTimeMillis();
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    /**
     * 增加连接计数
     */
    public void incrementConnection() {
        connectionCount++;
    }

    /**
     * 减少连接计数
     */
    public void decrementConnection() {
        if (connectionCount > 0) {
            connectionCount--;
        }
    }

    /**
     * 更新心跳时间
     */
    public void updateHeartbeat() {
        this.lastHeartbeatTime = System.currentTimeMillis();
        this.active = true;
    }

    /**
     * 检查是否超时
     */
    public boolean isTimeout(long timeoutMs) {
        return System.currentTimeMillis() - lastHeartbeatTime > timeoutMs;
    }
}
