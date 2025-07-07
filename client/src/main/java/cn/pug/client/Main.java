package cn.pug.client;

import cn.pug.client.component.init.Init;

public class Main {
    public static void main(String[] args) {
        Init init = new Init("127.0.0.1",8080);
        init.start();
    }
}
