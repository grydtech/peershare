package com.grydtech.peershare.bootstrap;

public class BootstrapServer {

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 4000;

        Server server = new Server(port);

        server.start();
    }
}
