package com.grydtech.peershare.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private static final int nodeLimit = 30;
    private static final Random random = new Random();

    private final List<Node> knownNodes = new ArrayList<>();

    private int port;

    public Server(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try{
            ServerSocket serverSocket = new ServerSocket(port);

            LOGGER.info("Bootstrap server started with port: {}", port);

            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    String request = in.readLine();

                    LOGGER.info("REQUEST received: \"{}\"", request);

                    String command = request.split(" ")[1];

                    String response;

                    if ("REG".equals(command)) {
                        response = handleREG(request);
                    } else if ("UNREG".equals(command)) {
                        response = handleUNREG(request);
                    } else {
                        throw new UnsupportedOperationException();
                    }

                    out.println(response);

                    LOGGER.info("RESPONSE sent: \"{}\"", response);
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private String handleREG(String request) {
        String[] parts = request.split(" ");
        String ip = parts[2];
        int port = Integer.parseInt(parts[3]);
        String username = parts[4];
        Node newNode = new Node(ip, port, username);

        if (knownNodes.size() >= nodeLimit) {
            LOGGER.error("failed, can’t register. BS full");
            return "0015 REGOK 9996";
        }

        Optional<Node> optionalNode = knownNodes.stream().filter(n -> n.equals(newNode)).findFirst();

        if (optionalNode.isPresent()) {
            if (optionalNode.get().getUsername().equals(newNode.getUsername())) {
                LOGGER.error("failed, already registered to you, unregister first");
                return "0015 REGOK 9998";
            } else {
                LOGGER.error("failed, registered to another user, try a different IP and port");
                return "0015 REGOK 9997";
            }
        }

        String response;

        int bound = knownNodes.size();

        if (bound == 0) {
            LOGGER.info("request is successful, no nodes in the system");
            response = "0012 REGOK 0";
        } else if (bound == 1) {
            String s = String.format("REGOK 1 %s %d", knownNodes.get(0).getIp(), knownNodes.get(0).getPort());

            LOGGER.info("request is successful, 1 node will be returned");
            response = String.format("%04d %s", s.length() + 5, s);
        } else {
            int index1 = random.nextInt(bound);
            int index2 = random.nextInt(bound);

            while (index1 == index2) {
                index2 = random.nextInt(bound);
            }

            String s = String.format("REGOK 2 %s %d %s %d",
                    knownNodes.get(index1).getIp(),
                    knownNodes.get(index1).getPort(),
                    knownNodes.get(index2).getIp(),
                    knownNodes.get(index2).getPort()
            );

            response = String.format("%04d %s", s.length() + 5, s);
        }

        knownNodes.add(newNode);

        LOGGER.info("request is successful, 2 nodes will be returned");
        return response;
    }

    private String handleUNREG(String request) {
        String[] parts = request.split(" ");
        String ip = parts[2];
        int port = Integer.parseInt(parts[3]);
        String username = parts[4];
        Node node = new Node(ip, port, username);

        Optional<Node> optionalNode = knownNodes.stream().filter(n -> n.equals(node)).findFirst();

        if (optionalNode.isPresent()) {
            knownNodes.remove(optionalNode.get());
            return "0012 UNROK 0";
        } else {
            return "0015 UNROK 9999";
        }
    }
}
