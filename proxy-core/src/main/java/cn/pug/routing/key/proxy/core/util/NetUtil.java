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
public class NetUtil {
    private static final AtomicInteger usePort = new AtomicInteger(9000);
    public static int getAvailablePort() {
        for (; ; ) {
            try (ServerSocket socket = new ServerSocket(usePort.get())) {
                return usePort.get();
            } catch (Exception e) {
                log.info("端口【{}】被占用", usePort.getAndIncrement());
            }
        }

    }
}
