package com.grydtech.peershare.distributed.services.impl;

import com.grydtech.peershare.distributed.exceptions.BootstrapException;
import com.grydtech.peershare.distributed.exceptions.IllegalCommandException;
import com.grydtech.peershare.distributed.helpers.NodeHelper;
import com.grydtech.peershare.distributed.models.ClientState;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.distributed.models.bootstrap.*;
import com.grydtech.peershare.distributed.models.gossip.GossipMessage;
import com.grydtech.peershare.distributed.models.heartbeat.HeartBeatMessage;
import com.grydtech.peershare.distributed.models.peer.PeerJoinRequest;
import com.grydtech.peershare.distributed.models.peer.PeerLeaveRequest;
import com.grydtech.peershare.distributed.models.peer.PeerResponseStatus;
import com.grydtech.peershare.distributed.services.ClusterManager;
import com.grydtech.peershare.distributed.services.JoinLeaveManager;
import com.grydtech.peershare.shared.services.TCPMessageSender;
import com.grydtech.peershare.shared.services.UDPMessageSender;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ClusterManagerImpl implements ClusterManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerImpl.class);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService joinExecutor = Executors.newSingleThreadScheduledExecutor();

    private final List<Node> bootstrapNodes = new ArrayList<>();
    private final List<Node> knownNodes = new ArrayList<>();
    private final BehaviorSubject<List<Node>> knownNodesBehaviourSubject = BehaviorSubject.create();

    @Value("${node.ttl}")
    private int nodeTTL;
    @Value("${node.heartbeat-interval}")
    private int nodeHeartBeatInterval;
    @Value("${join.timeout}")
    private int joinTimeout;
    @Value("${gossip.interval}")
    private int gossipInterval;
    @Value("${username}")
    private String username;

    private final JoinLeaveManager joinLeaveManager;
    private final TCPMessageSender tcpMessageSender;
    private final UDPMessageSender udpMessageSender;
    private final Node myNode;
    private final Node bootstrapNode;

    private ClientState clientState = ClientState.DISCONNECTED;

    @Autowired
    public ClusterManagerImpl(JoinLeaveManager joinLeaveManager, Node myNode, TCPMessageSender tcpMessageSender, UDPMessageSender udpMessageSender, Node bootstrapNode) {
        this.joinLeaveManager = joinLeaveManager;
        this.myNode = myNode;
        this.tcpMessageSender = tcpMessageSender;
        this.udpMessageSender = udpMessageSender;
        this.bootstrapNode = bootstrapNode;
    }

    @Override
    public synchronized void register() throws IOException, BootstrapException {
        RegisterResponse registerResponse = sendRegisterRequest();

        switch (registerResponse.getStatus()) {
            case ERROR:
                throw new BootstrapException("DISTRIBUTED: invalid command, please check again");
            case ALREADY_REGISTERED:
                throw new BootstrapException("DISTRIBUTED: already registered");
            case BOOTSTRAP_SERVER_FULL:
                throw new BootstrapException("DISTRIBUTED: bootstrap server full");
            case UNKNOWN:
                throw new BootstrapException("DISTRIBUTED: unknown error");
        }

        LOGGER.info("DISTRIBUTED: node registered with bootstrap server");
        this.clientState = ClientState.IDLE;

        this.bootstrapNodes.clear();

        LOGGER.info("DISTRIBUTED: bootstrap nodes available: \"{}\"", registerResponse.getNodes().size());
        this.bootstrapNodes.addAll(registerResponse.getNodes());
    }

    @Override
    public synchronized void unregister() throws IOException, IllegalCommandException {
        if (this.clientState == ClientState.CONNECTED) {
            throw new IllegalCommandException("DISTRIBUTED: node still connected to cluster, please leave first");
        }

        UnregisterResponse unregisterResponse = sendUnregisterRequest();

        if (unregisterResponse.getStatus() == BootstrapResponseStatus.SUCCESSFUL) {
            this.clientState = ClientState.UNREGISTERED;
            LOGGER.info("DISTRIBUTED: node unregistered with bootstrap server");

            this.bootstrapNodes.clear();
        }
    }

    @Override
    public synchronized void join() throws IllegalCommandException, IOException {
        if (this.clientState == ClientState.UNREGISTERED) {
            throw new IllegalCommandException("DISTRIBUTED: node already unregistered, please register again");
        }

        for (Node n : NodeHelper.getRandomNodes(this.bootstrapNodes)) {
            this.submitJoinRequest(n);
        }
    }

    @Override
    public synchronized void leave() throws IOException {
        for (Node n : this.knownNodes) {
            this.submitLeaveRequest(n);
        }
        this.knownNodes.clear();
        this.knownNodesBehaviourSubject.onNext(this.knownNodes);

        LOGGER.info("DISTRIBUTED: node disconnected from cluster");

        this.clientState = ClientState.DISCONNECTED;
    }

    @Override
    public synchronized void nodeConnected(Node connectedNode) {
        Optional<Node> node = this.knownNodes.stream().filter(n -> n.getId().equals(connectedNode.getId())).findFirst();

        if (!node.isPresent()) {
            connectedNode.startTTL(nodeTTL);
            this.knownNodes.add(connectedNode);
            this.knownNodesBehaviourSubject.onNext(this.knownNodes);

            LOGGER.info("DISTRIBUTED: connected node: \"{}\" added to cluster", connectedNode.getId());
        } else {
            node.get().resetTTL();
            LOGGER.trace("DISTRIBUTED: node: \"{}\" already connected", connectedNode.getId());
        }
    }

    @Override
    public synchronized void nodeDisconnected(Node disconnectedNode) {
        this.knownNodes.removeIf(n -> n.getId().equals(disconnectedNode.getId()));
        this.knownNodesBehaviourSubject.onNext(this.knownNodes);

        LOGGER.info("DISTRIBUTED: disconnected node: \"{}\" removed from cluster", disconnectedNode.getId());
    }

    @Override
    public synchronized void nodeDiscovered(Node discoveredNode) throws IOException {
        Optional<Node> node = this.knownNodes.stream().filter(n -> n.getId().equals(discoveredNode.getId())).findFirst();

        if (!node.isPresent()) {
            LOGGER.info("DISTRIBUTED: send join request to: \"{}\"", discoveredNode.getId());

            this.submitJoinRequest(discoveredNode);
        }
    }

    @Override
    public synchronized void nodeReset(Node aliveNode) throws IOException {
        Optional<Node> node = this.knownNodes.stream().filter(n -> n.getId().equals(aliveNode.getId())).findFirst();

        if (node.isPresent()) {
            LOGGER.trace("DISTRIBUTED: node: \"{}\" ttl reset", aliveNode.getId());

            node.get().resetTTL();
        } else {
            LOGGER.warn("DISTRIBUTED: node disconnected, retrying connection");

            this.submitJoinRequest(aliveNode);
        }
    }

    @Override
    public Observable<List<Node>> getConnectedClusterObservable() {
        return this.knownNodesBehaviourSubject;
    }

    @Override
    public List<Node> getConnectedCluster() {
        return this.knownNodes;
    }

    @Override
    public void startService() {
        LOGGER.info("DISTRIBUTED: distributed manager started");

        startJoinRetry();
        startTTLScanner();
        startHeartBeatSender();
        startGossipSender();
    }

    @Override
    public void stopService() {
        this.scheduledExecutorService.shutdown();

        LOGGER.info("DISTRIBUTED: distributed manager stopped");
    }

    private void startJoinRetry() {
        this.joinExecutor.scheduleAtFixedRate(() -> {
            synchronized (this) {
                if (clientState == ClientState.IDLE && !bootstrapNodes.isEmpty() && knownNodes.isEmpty()) {
                    LOGGER.warn("DISTRIBUTED: node is idle, retry join");

                    for (Node n : NodeHelper.getRandomNodes(this.bootstrapNodes)) {
                        try {
                            this.submitJoinRequest(n);
                        } catch (IOException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }
                } else {
                    LOGGER.info("DISTRIBUTED: join with cluster completed, shutting down join retry manager");

                    this.joinExecutor.shutdown();
                }
            }
        }, joinTimeout, joinTimeout, TimeUnit.SECONDS);

        LOGGER.info("DISTRIBUTED: join retry manager started");
    }

    private void startTTLScanner() {
        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            synchronized (this) {
                this.knownNodes.forEach(Node::reduceTTL);

                LOGGER.trace("DISTRIBUTED: node ttl cycle completed");
            }
        }, 1, 1, TimeUnit.SECONDS);

        LOGGER.info("DISTRIBUTED: node ttl reducer started");

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            synchronized (this) {
                List<Node> unresponsiveNodes = this.knownNodes.stream().filter(Node::isTTLExpired)
                        .collect(Collectors.toList());
                this.knownNodes.removeAll(unresponsiveNodes);

                if (!unresponsiveNodes.isEmpty()) {
                    this.knownNodesBehaviourSubject.onNext(this.knownNodes);
                }

                unresponsiveNodes.forEach(un -> {
                    try {
                        this.submitJoinRequest(un);
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                });

                LOGGER.trace("DISTRIBUTED: node ttl scanned");
            }
        }, nodeTTL, nodeTTL, TimeUnit.SECONDS);

        LOGGER.info("DISTRIBUTED: node ttl scanner started");
    }

    private void startHeartBeatSender() {
        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            synchronized (this) {
                this.knownNodes.forEach(n -> {
                    try {
                        sendHeartBeatMessage(n);
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                });
            }
        }, 0, nodeHeartBeatInterval, TimeUnit.SECONDS);

        LOGGER.info("DISTRIBUTED: node alive gossip sender started");
    }

    private void startGossipSender() {
        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            List<Node> discoveredNodes = NodeHelper.getRandomNodes(knownNodes);
            List<Node> destinationNodea = NodeHelper.getRandomNodes(knownNodes);

            for (Node dn : discoveredNodes) {
                for (Node n : destinationNodea) {
                    try {
                        sendNodeDiscoveredGossip(dn, n);
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        }, gossipInterval, gossipInterval, TimeUnit.SECONDS);

        LOGGER.info("DISTRIBUTED: gossip sender started");
    }

    private RegisterResponse sendRegisterRequest() throws IOException {
        RegisterRequest registerRequest = new RegisterRequest(myNode, username);

        LOGGER.info("DISTRIBUTED: send register request");

        String response = tcpMessageSender.sendMessage(registerRequest, bootstrapNode);

        RegisterResponse registerResponse = new RegisterResponse();
        registerResponse.deserialize(response);

        LOGGER.info("DISTRIBUTED: register response received: \"{}\"", registerResponse.getStatus().toString());

        return registerResponse;
    }

    private UnregisterResponse sendUnregisterRequest() throws IOException {
        UnregisterRequest unregisterRequest = new UnregisterRequest(myNode, username);

        LOGGER.info("DISTRIBUTED: send unregister request");

        String response = tcpMessageSender.sendMessage(unregisterRequest, bootstrapNode);

        UnregisterResponse unregisterResponse = new UnregisterResponse();
        unregisterResponse.deserialize(response);

        LOGGER.info("DISTRIBUTED: unregister response received: \"{}\"", unregisterResponse.getStatus().toString());

        return unregisterResponse;
    }

    private void sendNodeDiscoveredGossip(Node discoveredNode, Node destinationNode) throws IOException {
        GossipMessage gossipMessage = new GossipMessage(discoveredNode);

        if (discoveredNode.getId().equals(destinationNode.getId())) {
            LOGGER.trace("DISTRIBUTED: cannot send node: \"{}\" discovered gossip to same node: \"{}\"", discoveredNode.getId(), destinationNode.getId());
            return;
        }

        LOGGER.info("DISTRIBUTED: send node: \"{}\" discovered gossip to: \"{}\"", discoveredNode.getId(), destinationNode.getId());

        udpMessageSender.sendMessage(gossipMessage, destinationNode);
    }

    private void sendHeartBeatMessage(Node destinationNode) throws IOException {
        HeartBeatMessage heartBeatMessage = new HeartBeatMessage(myNode);

        LOGGER.info("DISTRIBUTED: send heart beat message to: \"{}\"", destinationNode.getId());
        udpMessageSender.sendMessage(heartBeatMessage, destinationNode);
    }

    private void submitJoinRequest(Node destinationNode) throws IOException {
        PeerJoinRequest peerJoinRequest = new PeerJoinRequest(myNode);

        joinLeaveManager.submitJoinRequest(peerJoinRequest, destinationNode).subscribe(status -> {
            if (status == PeerResponseStatus.SUCCESSFUL) nodeConnected(destinationNode);
        });
    }

    private void submitLeaveRequest(Node destinationNode) throws IOException {
        PeerLeaveRequest peerLeaveRequest = new PeerLeaveRequest(myNode);

        joinLeaveManager.submitLeaveRequest(peerLeaveRequest, destinationNode).subscribe(status -> {
            if (status == PeerResponseStatus.SUCCESSFUL) nodeDisconnected(destinationNode);
        });
    }
}
