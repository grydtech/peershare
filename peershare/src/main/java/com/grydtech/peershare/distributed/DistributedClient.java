package com.grydtech.peershare.distributed;

import com.grydtech.peershare.distributed.models.Command;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.distributed.models.gossip.NodeDiscoveredGossip;
import com.grydtech.peershare.distributed.models.gossip.NodeUnresponsiveGossip;
import com.grydtech.peershare.distributed.models.gossip.NodeAliveGossip;
import com.grydtech.peershare.distributed.models.peer.PeerJoinRequest;
import com.grydtech.peershare.distributed.models.peer.PeerJoinResponse;
import com.grydtech.peershare.distributed.models.peer.PeerLeaveRequest;
import com.grydtech.peershare.distributed.models.peer.PeerLeaveResponse;
import com.grydtech.peershare.distributed.models.search.FileSearchRequest;
import com.grydtech.peershare.distributed.models.search.FileSearchResponse;
import com.grydtech.peershare.distributed.services.ClusterManager;
import com.grydtech.peershare.distributed.services.FileSearchManager;
import com.grydtech.peershare.distributed.services.MessageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class DistributedClient extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedClient.class);

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private final DatagramSocket udpSocket;
    private final ClusterManager clusterManager;
    private final MessageManager messageManager;
    private final FileSearchManager fileSearchManager;

    @Autowired
    public DistributedClient(DatagramSocket udpSocket, ClusterManager clusterManager, MessageManager messageManager, FileSearchManager fileSearchManager) {
        this.udpSocket = udpSocket;
        this.clusterManager = clusterManager;
        this.messageManager = messageManager;
        this.fileSearchManager = fileSearchManager;
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                clusterManager.leave();
                clusterManager.unregister();

                clusterManager.stopService();
                fileSearchManager.stopService();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }));

        try {
            clusterManager.startService();
            fileSearchManager.startService();

            clusterManager.unregister();
            clusterManager.register();
            clusterManager.join();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            Runtime.getRuntime().exit(0);
        }

        byte[] buf = new byte[256];

        LOGGER.info("message acceptor started");

        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                try {
                    udpSocket.receive(packet);
                } catch (SocketException e) {
                    LOGGER.error(e.getMessage(), e);
                    break;
                }

                String received = new String(packet.getData(), 0, packet.getLength());

                LOGGER.trace("UDP packet received: \"{}\"", received);

                executorService.submit(() -> {
                    try {
                        decodeMessage(received);
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                });
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void decodeMessage(String message) throws IOException {
        String[] parts = message.split(" ");
        Command command = (parts.length < 3) ? Command.UNKNOWN : Command.fromString(parts[2]);

        switch (command) {
            case JOIN:
                handleJoinRequest(message);
                break;
            case JOIN_OK:
                handleJoinResponse(message);
                break;
            case LEAVE:
                handleLeaveRequest(message);
                break;
            case LEAVE_OK:
                handleLeaveResponse(message);
                break;
            case SEARCH:
                handleFileSearchRequest(message);
                break;
            case SEARCH_OK:
                handleFileSearchResponse(message);
                break;
            case NODE_DISCOVERED:
                handleNodeDiscoveredGossip(message);
                break;
            case NODE_UNRESPONSIVE:
                handleNodeUnresponsiveGossip(message);
                break;
            case NODE_ALIVE:
                handleHeartBeatMessage(message);
                break;
            case UNKNOWN:
                LOGGER.error("unknown command received");
                break;
        }
    }

    private void handleJoinRequest(String message) throws IOException {
        PeerJoinRequest peerJoinRequest = new PeerJoinRequest();
        peerJoinRequest.deserialize(message);

        LOGGER.info("join request received from: \"{}\"", peerJoinRequest.getNode().getId());

        clusterManager.nodeConnected(peerJoinRequest.getNode());
        messageManager.sendJoinResponse(peerJoinRequest.getNode(), peerJoinRequest.getMessageId());
    }

    private void handleJoinResponse(String message) throws IOException {
        PeerJoinResponse peerJoinResponse = new PeerJoinResponse();
        peerJoinResponse.deserialize(message);
        PeerJoinRequest peerJoinRequest = (PeerJoinRequest) messageManager.getSentMessageById(peerJoinResponse.getMessageId().toString());

        if (peerJoinRequest != null) {
            LOGGER.info("join response received from: \"{}\"", peerJoinRequest.getNode().getId());

            clusterManager.nodeConnected(peerJoinRequest.getNode());
        }
    }

    private void handleLeaveRequest(String message) throws IOException {
        PeerLeaveRequest peerLeaveRequest = new PeerLeaveRequest();
        peerLeaveRequest.deserialize(message);

        LOGGER.info("leave request received from: \"{}\"", peerLeaveRequest.getNode().getId());

        clusterManager.nodeDisconnected(peerLeaveRequest.getNode());
        messageManager.sendLeaveResponse(peerLeaveRequest.getNode(), peerLeaveRequest.getMessageId());
    }

    private void handleLeaveResponse(String message) {
        PeerLeaveResponse peerLeaveResponse = new PeerLeaveResponse();
        peerLeaveResponse.deserialize(message);

        PeerLeaveRequest peerLeaveRequest = (PeerLeaveRequest) messageManager.getSentMessageById(peerLeaveResponse.getMessageId().toString());

        if (peerLeaveRequest != null) {
            LOGGER.info("leave response received from: \"{}\"", peerLeaveRequest.getNode().getId());
        }
    }

    private void handleNodeDiscoveredGossip(String message) throws IOException {
        NodeDiscoveredGossip nodeDiscoveredGossip = new NodeDiscoveredGossip();
        nodeDiscoveredGossip.deserialize(message);

        LOGGER.info("node: \"{}\" discovered gossip received", nodeDiscoveredGossip.getDiscoveredNode().getId());

        clusterManager.nodeDiscovered(nodeDiscoveredGossip.getDiscoveredNode(), nodeDiscoveredGossip.getHop() + 1);
    }

    private void handleNodeUnresponsiveGossip(String message) throws IOException {
        NodeUnresponsiveGossip nodeUnresponsiveGossip = new NodeUnresponsiveGossip();
        nodeUnresponsiveGossip.deserialize(message);

        LOGGER.info("node: \"{}\" unresponsive gossip received", nodeUnresponsiveGossip.getUnresponsiveNode().getId());

        clusterManager.nodeUnresponsive(nodeUnresponsiveGossip.getUnresponsiveNode(), nodeUnresponsiveGossip.getHop() + 1);
    }

    private void handleHeartBeatMessage(String message) throws IOException {
        NodeAliveGossip nodeAliveGossip = new NodeAliveGossip();
        nodeAliveGossip.deserialize(message);

        LOGGER.trace("node: \"{}\" alive gossip received", nodeAliveGossip.getAliveNode().getId());

        clusterManager.nodeAlive(nodeAliveGossip.getAliveNode(), nodeAliveGossip.getHop() + 1);
    }

    private void handleFileSearchRequest(String message) throws IOException {
        FileSearchRequest fileSearchRequest = new FileSearchRequest();
        fileSearchRequest.deserialize(message);

        LOGGER.info("file search request: \"{}\" received from: \"{}\"", fileSearchRequest.getKeyword(), fileSearchRequest.getNode().getId());

        fileSearchManager.acceptSearchRequest(fileSearchRequest.getMessageId(), fileSearchRequest.getKeyword(), fileSearchRequest.getNode(), fileSearchRequest.getHop());
    }

    private void handleFileSearchResponse(String message) {
        FileSearchResponse fileSearchResponse = new FileSearchResponse();
        fileSearchResponse.deserialize(message);

        LOGGER.info("file search response: \"{}\" received from: \"{}\"", fileSearchResponse.getStatus().toString(), fileSearchResponse.getNode().getId());

        fileSearchManager.submitSearchResult(fileSearchResponse.getMessageId(), fileSearchResponse.getFileNames(), fileSearchResponse.getNode(), fileSearchResponse.getHops());
    }
}
