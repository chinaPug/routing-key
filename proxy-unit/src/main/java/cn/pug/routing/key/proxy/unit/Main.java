package cn.pug.routing.key.proxy.unit;

import cn.pug.routing.key.proxy.unit.component.daemon.Daemon;

public class Main {
    public static void main(String[] args) {
        Daemon daemon = new Daemon("172.31.169.140",11111,"172.31.169.105");
        daemon.start();
    }
}
