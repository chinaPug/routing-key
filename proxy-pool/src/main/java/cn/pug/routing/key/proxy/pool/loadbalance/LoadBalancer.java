package cn.pug.routing.key.proxy.pool.loadbalance;

import cn.pug.routing.key.proxy.pool.component.ServerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 代理节点负载均衡器
 * 
 * 支持多种负载均衡策略：轮询、随机、最少连接、加权
 * 
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class LoadBalancer {

    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * 负载均衡策略枚举
     */
    public enum Strategy {
        ROUND_ROBIN,    // 轮询
        RANDOM,         // 随机
        LEAST_CONNECTIONS,  // 最少连接
        WEIGHTED        // 加权
    }

    private Strategy strategy = Strategy.ROUND_ROBIN;

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
        log.info("负载均衡策略切换为: {}", strategy);
    }

    /**
     * 选择一个代理节点
     */
    public ServerContext.UnitInstance select(List<ServerContext.UnitInstance> units) {
        if (units == null || units.isEmpty()) {
            return null;
        }

        // 过滤掉不可用的节点
        List<ServerContext.UnitInstance> availableUnits = units.stream()
                .filter(ServerContext.UnitInstance::isActive)
                .toList();

        if (availableUnits.isEmpty()) {
            log.warn("没有可用的代理节点");
            return null;
        }

        return switch (strategy) {
            case ROUND_ROBIN -> roundRobin(availableUnits);
            case RANDOM -> random(availableUnits);
            case LEAST_CONNECTIONS -> leastConnections(availableUnits);
            case WEIGHTED -> weighted(availableUnits);
        };
    }

    /**
     * 轮询策略
     */
    private ServerContext.UnitInstance roundRobin(List<ServerContext.UnitInstance> units) {
        String key = "global";
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % units.size();
        return units.get(Math.abs(index));
    }

    /**
     * 随机策略
     */
    private ServerContext.UnitInstance random(List<ServerContext.UnitInstance> units) {
        return units.get(random.nextInt(units.size()));
    }

    /**
     * 最少连接策略
     */
    private ServerContext.UnitInstance leastConnections(List<ServerContext.UnitInstance> units) {
        return units.stream()
                .min((u1, u2) -> Integer.compare(u1.getConnectionCount(), u2.getConnectionCount()))
                .orElse(units.get(0));
    }

    /**
     * 加权策略（根据节点权重选择）
     */
    private ServerContext.UnitInstance weighted(List<ServerContext.UnitInstance> units) {
        int totalWeight = units.stream().mapToInt(ServerContext.UnitInstance::getWeight).sum();
        if (totalWeight <= 0) {
            return random(units);
        }

        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (ServerContext.UnitInstance unit : units) {
            currentWeight += unit.getWeight();
            if (randomWeight < currentWeight) {
                return unit;
            }
        }

        return units.get(units.size() - 1);
    }

    /**
     * 获取当前策略
     */
    public Strategy getStrategy() {
        return strategy;
    }
}
