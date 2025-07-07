package cn.pug.server;

import cn.pug.server.component.ServerContext;

public class Main {
    public static void main(String[] args) {
        ServerContext serverContext = new ServerContext(8080);
        serverContext.start();
    }
}
