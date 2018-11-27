package com.grydtech.peershare;

import com.grydtech.peershare.distributed.models.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Objects;

@Configuration
public class BeanConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanConfiguration.class);

    private final Environment environment;

    @Autowired
    public BeanConfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public Node myNode() throws IOException {
        String serverHost = environment.getProperty("server.host");
        int serverPort = Integer.parseInt(Objects.requireNonNull(environment.getProperty("server.port")));

        if (serverHost == null || serverHost.equals("")) {
            LOGGER.info("server host not found retrieving host");

            InetAddress localhost = InetAddress.getLocalHost();
            serverHost = localhost.getHostAddress().trim();

            LOGGER.info("server host retrieved: \"{}\"", serverHost);
        }

        return new Node(serverHost, serverPort);
    }

    @Bean
    public Node bootstrapNode() {
        String bootstrapHost = environment.getProperty("bootstrap.host");
        int bootstrapPort = Integer.parseInt(Objects.requireNonNull(environment.getProperty("bootstrap.port")));
        return new Node(bootstrapHost, bootstrapPort);
    }

    @Bean
    public DatagramSocket udpSocket(Node myNode) throws SocketException {
        return new DatagramSocket(myNode.getUdpPort());
    }
}
