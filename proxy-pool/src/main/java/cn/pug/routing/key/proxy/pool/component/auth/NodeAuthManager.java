package cn.pug.routing.key.proxy.pool.component.auth;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代理节点认证管理器
 * 
 * 防止恶意节点注册，支持 Token 认证
 * 
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class NodeAuthManager {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_EXPIRE_SECONDS = 3600; // Token 1小时过期
    
    private final String secretKey;
    private final Set<String> whitelist;
    private final Map<String, Long> tokenCache = new ConcurrentHashMap<>();

    public NodeAuthManager(String secretKey) {
        this(secretKey, null);
    }

    public NodeAuthManager(String secretKey, Set<String> whitelist) {
        this.secretKey = secretKey;
        this.whitelist = whitelist;
    }

    /**
     * 生成认证 Token
     */
    public String generateToken(String nodeId) {
        try {
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String data = nodeId + ":" + timestamp;
            
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(hash);
            
            String token = nodeId + ":" + timestamp + ":" + signature;
            tokenCache.put(nodeId, Instant.now().getEpochSecond());
            
            log.info("为节点 [{}] 生成认证 Token", nodeId);
            return token;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("生成 Token 失败: {}", e.getMessage());
            throw new RuntimeException("Token 生成失败", e);
        }
    }

    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        String[] parts = token.split(":");
        if (parts.length != 3) {
            log.warn("Token 格式错误");
            return false;
        }

        String nodeId = parts[0];
        String timestamp = parts[1];
        String signature = parts[2];

        // 检查白名单
        if (whitelist != null && !whitelist.isEmpty() && !whitelist.contains(nodeId)) {
            log.warn("节点 [{}] 不在白名单中", nodeId);
            return false;
        }

        // 检查 Token 是否过期
        long tokenTime = Long.parseLong(timestamp);
        long currentTime = Instant.now().getEpochSecond();
        if (currentTime - tokenTime > TOKEN_EXPIRE_SECONDS) {
            log.warn("节点 [{}] 的 Token 已过期", nodeId);
            return false;
        }

        // 验证签名
        try {
            String data = nodeId + ":" + timestamp;
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);
            
            if (!expectedSignature.equals(signature)) {
                log.warn("节点 [{}] 的 Token 签名无效", nodeId);
                return false;
            }

            log.debug("节点 [{}] 的 Token 验证通过", nodeId);
            return true;
        } catch (Exception e) {
            log.error("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 使 Token 失效
     */
    public void invalidateToken(String nodeId) {
        tokenCache.remove(nodeId);
        log.info("节点 [{}] 的 Token 已失效", nodeId);
    }
}
