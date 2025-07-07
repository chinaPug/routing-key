package cn.pug.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

@Slf4j
public class NetUtil {
    public static boolean isPortCanUse(int port){
        try (ServerSocket serverSocket = new ServerSocket(port)){
            log.info("端口【{}】可用",port);
            return true;
        } catch (Exception e) {
            log.info("端口【{}】不可用",port);
            return false;
        }
    }

    /**
     * 获取本机IP地址
     * @return 本机IP地址字符串
     */
    public static String getLocalIP() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("无法获取本机IP地址", e);
        }
    }
}