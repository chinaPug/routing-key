package cn.pug.client;

import cn.pug.client.component.init.Daemon;

public class Main {
    public static void main(String[] args) {
        Daemon daemon = new Daemon("127.0.0.1",8080);
        daemon.start();
    }
}
