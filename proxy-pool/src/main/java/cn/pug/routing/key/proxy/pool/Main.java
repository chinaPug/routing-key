package cn.pug.routing.key.proxy.pool;

import cn.pug.routing.key.proxy.pool.component.ServerContext;

public class Main {
    public static void main(String[] args) {
        // 配置本机ip
        ServerContext.ServerContextHolder.INSTANCE.getServerContext().start();
    }
}
