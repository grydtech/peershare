package com.grydtech.peershare;

import com.grydtech.peershare.distributed.models.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.*;
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

            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.connect(new InetSocketAddress("8.8.8.8", 10002));
                serverHost = socket.getLocalAddress().getHostAddress().trim();
            }
        }

        if ("0.0.0.0".equals(serverHost) || "127.0.0.1".equals(serverHost)) {
            InetAddress localhost = InetAddress.getLocalHost();
            serverHost = localhost.getHostAddress().trim();
        }

        LOGGER.info("server host retrieved: \"{}\"", serverHost);

        if (environment.containsProperty("username")) {
            String username = environment.getProperty("username");
            return new Node(serverHost, serverPort, username);
        } else {
            return new Node(serverHost, serverPort);
        }
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
