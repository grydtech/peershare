package com.grydtech.peershare.distributed;

import com.grydtech.peershare.distributed.models.Command;
import com.grydtech.peershare.distributed.models.gossip.GossipMessage;
import com.grydtech.peershare.distributed.models.heartbeat.HeartBeatMessage;
import com.grydtech.peershare.distributed.models.peer.PeerJoinRequest;
import com.grydtech.peershare.distributed.models.peer.PeerJoinResponse;
import com.grydtech.peershare.distributed.models.peer.PeerLeaveRequest;
import com.grydtech.peershare.distributed.models.peer.PeerLeaveResponse;
import com.grydtech.peershare.distributed.models.search.FileSearchRequest;
import com.grydtech.peershare.distributed.models.search.FileSearchResponse;
import com.grydtech.peershare.distributed.services.ClusterManager;
import com.grydtech.peershare.distributed.services.FileSearchManager;
import com.grydtech.peershare.distributed.services.JoinLeaveManager;
import com.grydtech.peershare.report.services.Reporter;
import com.grydtech.peershare.shared.services.UDPMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class DistributedClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedClient.class);

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private final UDPMessageListener udpMessageListener;
    private final ClusterManager clusterManager;
    private final JoinLeaveManager joinLeaveManager;
    private final FileSearchManager fileSearchManager;
    private final Reporter reporter;

    @Autowired
    public DistributedClient(UDPMessageListener udpMessageListener, ClusterManager clusterManager,
                             JoinLeaveManager joinLeaveManager, FileSearchManager fileSearchManager, Reporter reporter) {
        this.udpMessageListener = udpMessageListener;
        this.clusterManager = clusterManager;
        this.joinLeaveManager = joinLeaveManager;
        this.fileSearchManager = fileSearchManager;
        this.reporter = reporter;
    }

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                clusterManager.leave();
                clusterManager.unregister();

                clusterManager.stopService();
                joinLeaveManager.stopService();
                fileSearchManager.stopService();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }));

        try {
            clusterManager.startService();
            joinLeaveManager.startService();
            fileSearchManager.startService();

            clusterManager.unregister();
            clusterManager.register();
            clusterManager.join();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            Runtime.getRuntime().exit(0);
        }

        udpMessageListener.listen().subscribe(this::decodeMessage);
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
            case GOSSIP:
                handleNodeDiscoveredGossip(message);
                break;
            case HEART_BEAT:
                handleNodeHeartBeatMessage(message);
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
        joinLeaveManager.acceptJoinRequest(peerJoinRequest.getMessageId(), peerJoinRequest.getNode());
    }

    private void handleJoinResponse(String message) {
        PeerJoinResponse peerJoinResponse = new PeerJoinResponse();
        peerJoinResponse.deserialize(message);

        LOGGER.info("join response received");

        joinLeaveManager.submitResponse(peerJoinResponse.getMessageId(), peerJoinResponse.getStatus());
    }

    private void handleLeaveRequest(String message) throws IOException {
        PeerLeaveRequest peerLeaveRequest = new PeerLeaveRequest();
        peerLeaveRequest.deserialize(message);

        LOGGER.info("leave request received from: \"{}\"", peerLeaveRequest.getNode().getId());

        clusterManager.nodeDisconnected(peerLeaveRequest.getNode());
        joinLeaveManager.acceptLeaveRequest(peerLeaveRequest.getMessageId(), peerLeaveRequest.getNode());
    }

    private void handleLeaveResponse(String message) {
        PeerLeaveResponse peerLeaveResponse = new PeerLeaveResponse();
        peerLeaveResponse.deserialize(message);

        LOGGER.info("leave response received");

        joinLeaveManager.submitResponse(peerLeaveResponse.getMessageId(), peerLeaveResponse.getStatus());
    }

    private void handleNodeDiscoveredGossip(String message) throws IOException {
        GossipMessage gossipMessage = new GossipMessage();
        gossipMessage.deserialize(message);

        LOGGER.info("node: \"{}\" discovered gossip received", gossipMessage.getDiscoveredNode().getId());

        clusterManager.nodeDiscovered(gossipMessage.getDiscoveredNode());
    }

    private void handleNodeHeartBeatMessage(String message) throws IOException {
        HeartBeatMessage heartBeatMessage = new HeartBeatMessage();
        heartBeatMessage.deserialize(message);

        LOGGER.trace("node: \"{}\" alive gossip received", heartBeatMessage.getNode().getId());

        clusterManager.nodeReset(heartBeatMessage.getNode());
    }

    private void handleFileSearchRequest(String message) throws IOException {
        FileSearchRequest fileSearchRequest = new FileSearchRequest();
        fileSearchRequest.deserialize(message);

        LOGGER.info("file search request: \"{}\" received from: \"{}\"", fileSearchRequest.getKeyword(), fileSearchRequest.getNode().getId());

        fileSearchManager.acceptSearchRequest(fileSearchRequest.getMessageId(), fileSearchRequest.getKeyword(), fileSearchRequest.getNode(), fileSearchRequest.getHop());

        reporter.reportSearchAccepted(fileSearchRequest.getMessageId(), fileSearchRequest.getHop());
    }

    private void handleFileSearchResponse(String message) {
        FileSearchResponse fileSearchResponse = new FileSearchResponse();
        fileSearchResponse.deserialize(message);

        LOGGER.info("file search response: \"{}\" received from: \"{}\"", fileSearchResponse.getStatus().toString(), fileSearchResponse.getNode().getId());

        fileSearchManager.submitSearchResult(fileSearchResponse.getMessageId(), fileSearchResponse.getFileNames(), fileSearchResponse.getNode(), fileSearchResponse.getHops());

        reporter.reportResultReceived(fileSearchResponse.getMessageId(), fileSearchResponse.getFileNames().size(), fileSearchResponse.getHops(), fileSearchResponse.getNode().getId());
    }
}
