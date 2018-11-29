package com.grydtech.peershare;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class NetworkTest {
    public static void main(String[] args) throws UnknownHostException, SocketException {
        System.out.println(InetAddress.getLocalHost().getHostAddress());
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(new InetSocketAddress("8.8.8.8", 10002));
            System.out.println(socket.getLocalAddress().getHostAddress());
        }
    }
}
