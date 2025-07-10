package cn.pug.server;

import cn.pug.server.component.ServerContext;

public class Main {
    public static void main(String[] args) {
        // 配置本机ip
        ServerContext serverContext = new ServerContext("172.31.169.140",11111);
        serverContext.start();
    }
}
