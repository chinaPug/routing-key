package cn.pug.routing.key.proxy.core.util;

import lombok.extern.slf4j.Slf4j;

import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 网络工具类
 *
 * @author pug
 */
@Slf4j
public final class NetUtil {
    
    private static final AtomicInteger usePort = new AtomicInteger(9000);
    private static final int MAX_PORT = 65535;
    
    /**
     * 私有构造函数，防止实例化工具类
     */
    private NetUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 获取可用端口
     * 
     * @return 可用的端口号
     * @throws RuntimeException 当无可用端口时抛出异常
     */
    public static int getAvailablePort() {
        int attempts = 0;
        int currentPort = usePort.get();
        
        while (attempts < (MAX_PORT - currentPort)) {
            int portToTry = usePort.getAndIncrement();
            
            // 端口范围检查
            if (portToTry > MAX_PORT) {
                usePort.set(9000); // 重置到起始端口
                portToTry = usePort.getAndIncrement();
            }
            
            try (ServerSocket socket = new ServerSocket(portToTry)) {
                log.debug("找到可用端口【{}】", portToTry);
                return portToTry;
            } catch (Exception e) {
                log.debug("端口【{}】被占用", portToTry);
                attempts++;
            }
        }
        
        throw new RuntimeException("无法找到可用端口，已尝试 " + attempts + " 次");
    }
}
