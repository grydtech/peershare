package com.grydtech.peershare;

import com.grydtech.peershare.distributed.models.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.DatagramSocket;
import java.net.SocketException;

@Configuration
public class BeanConfiguration {

    @Value(("${bootstrap.host}"))
    private String bootstrapHost;

    @Value(("${bootstrap.port}"))
    private int bootstrapPort;

    @Value(("${server.host}"))
    private String serverHost;

    @Value(("${server.port}"))
    private int serverPort;

    @Bean
    public Node myNode() {
        return new Node(serverHost, serverPort);
    }

    @Bean
    public Node bootstrapNode() {
        return new Node(bootstrapHost, bootstrapPort);
    }

    @Bean
    public DatagramSocket udpSocket(Node myNode) throws SocketException {
        return new DatagramSocket(myNode.getUdpPort());
    }
}
