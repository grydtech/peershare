package com.grydtech.peershare.distributed.services.impl;

import com.grydtech.peershare.distributed.exceptions.BootstrapException;
import com.grydtech.peershare.distributed.exceptions.IllegalCommandException;
import com.grydtech.peershare.distributed.helpers.NodeHelper;
import com.grydtech.peershare.distributed.models.ClientState;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.distributed.models.bootstrap.RegisterRequest;
import com.grydtech.peershare.distributed.models.bootstrap.RegisterResponse;
import com.grydtech.peershare.distributed.models.bootstrap.UnregisterRequest;
import com.grydtech.peershare.distributed.models.bootstrap.UnregisterResponse;
import com.grydtech.peershare.distributed.models.gossip.NodeAliveGossip;
import com.grydtech.peershare.distributed.models.gossip.NodeDiscoveredGossip;
import com.grydtech.peershare.distributed.models.gossip.NodeUnresponsiveGossip;
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

    private ClientState clientState = ClientState.DISCONNECTED;

    @Value("${node.ttl}")
    private int nodeTTL;

    @Value("${node.gossip-interval}")
    private int nodeGossipInterval;

    @Value("${join.timeout}")
    private int joinTimeout;

    @Value("${server.name}")
    private String serviceName;

    @Value("${gossip.max-hops}")
    private int gossipMaxHops;

    private final JoinLeaveManager joinLeaveManager;
    private final TCPMessageSender tcpMessageSender;
    private final UDPMessageSender udpMessageSender;
    private final Node myNode;
    private final Node bootstrapNode;

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
                throw new BootstrapException("invalid command, please check again");
            case ALREADY_REGISTERED:
                throw new BootstrapException("already registered");
            case BOOTSTRAP_SERVER_FULL:
                throw new BootstrapException("bootstrap server full");
            case UNKNOWN:
                throw new BootstrapException("unknown error");
        }

        LOGGER.info("node registered with bootstrap server");
        this.clientState = ClientState.IDLE;

        this.bootstrapNodes.clear();
        this.bootstrapNodes.addAll(registerResponse.getNodes());
    }

    @Override
    public synchronized void unregister() throws IOException, IllegalCommandException {
        if (this.clientState == ClientState.CONNECTED) {
            throw new IllegalCommandException("node still connected to cluster, please leave first");
        }

        UnregisterResponse unregisterResponse = sendUnregisterRequest();

        this.clientState = ClientState.UNREGISTERED;
        LOGGER.info("node unregistered with bootstrap server");

        this.bootstrapNodes.clear();
    }

    @Override
    public synchronized void join() throws IllegalCommandException, IOException {
        if (this.clientState == ClientState.UNREGISTERED) {
            throw new IllegalCommandException("node already unregistered, please register again");
        }

        for (Node n : NodeHelper.getRandomNodes(this.bootstrapNodes)) {
            sendJoinRequest(n);
        }
    }

    @Override
    public synchronized void leave() throws IOException {
        for (Node n : this.knownNodes) {
            sendJoinRequest(n);
        }
        this.knownNodes.clear();
        this.knownNodesBehaviourSubject.onNext(this.knownNodes);

        LOGGER.info("node disconnected from cluster");

        this.clientState = ClientState.DISCONNECTED;
    }

    @Override
    public synchronized void nodeConnected(Node connectedNode) throws IOException {
        Optional<Node> node = this.knownNodes.stream().filter(n -> n.getId().equals(connectedNode.getId())).findFirst();

        if (!node.isPresent()) {
            connectedNode.startTTL(nodeTTL);
            this.knownNodes.add(connectedNode);
            this.knownNodesBehaviourSubject.onNext(this.knownNodes);

            LOGGER.info("connected node: \"{}\" added to cluster", connectedNode.getId());

            for (Node n : this.knownNodes) {
                if (!n.getId().equals(connectedNode.getId())) {
                    sendNodeDiscoveredGossip(connectedNode, n, 1);
                }
            }
        } else {
            LOGGER.warn("node: \"{}\" already connected", connectedNode.getId());
        }
    }

    @Override
    public synchronized void nodeDisconnected(Node disconnectedNode) {
        this.knownNodes.removeIf(n -> n.getId().equals(disconnectedNode.getId()));
        this.knownNodesBehaviourSubject.onNext(this.knownNodes);

        LOGGER.warn("disconnected node: \"{}\" removed from cluster", disconnectedNode.getId());
    }

    @Override
    public synchronized void nodeDiscovered(Node discoveredNode, int hop) throws IOException {
        Optional<Node> node = this.knownNodes.stream().filter(n -> n.getId().equals(discoveredNode.getId())).findFirst();

        if (!node.isPresent()) {
            LOGGER.info("send join request to: \"{}\"", discoveredNode.getId());

            joinLeaveManager.submitJoinRequest(discoveredNode).subscribe(status -> {
                if (status) nodeConnected(discoveredNode);
            });

            LOGGER.info("select random nodes to send node discovered gossip");

            for (Node n : NodeHelper.getRandomNodes(this.knownNodes)) {
                sendNodeDiscoveredGossip(discoveredNode, n, hop + 1);
            }
        }
    }

    @Override
    public synchronized void nodeUnresponsive(Node unresponsiveNode, int hop) throws IOException {
        Optional<Node> node = this.knownNodes.stream().filter(n -> n.getId().equals(unresponsiveNode.getId())).findFirst();

        if (node.isPresent()) {
            this.knownNodes.removeIf(n -> n.getId().equals(unresponsiveNode.getId()));
            this.knownNodesBehaviourSubject.onNext(this.knownNodes);

            LOGGER.info("unresponsive node: \"{}\" removed from cluster", unresponsiveNode.getId());

            sendJoinRequest(unresponsiveNode);

            LOGGER.info("select random nodes to send node unresponsive gossip");

            for (Node n : NodeHelper.getRandomNodes(this.knownNodes)) {
                sendNodeUnresponsiveGossip(unresponsiveNode, n, hop + 1);
            }
        } else {
            LOGGER.warn("unresponsive node: \"{}\" already removed from cluster", unresponsiveNode.getId());
        }
    }

    @Override
    public synchronized void nodeAlive(Node aliveNode, int hop) throws IOException {
        Optional<Node> node = this.knownNodes.stream().filter(n -> n.getId().equals(aliveNode.getId())).findFirst();

        if (node.isPresent()) {
            LOGGER.trace("node: \"{}\" ttl reset", aliveNode.getId());

            node.get().resetTTL();

            LOGGER.trace("select random nodes to send node alive gossip");

            for (Node n : NodeHelper.getRandomNodes(this.knownNodes)) {
                sendNodeAliveGossip(aliveNode, n, hop + 1);
            }
        } else {
            LOGGER.warn("node disconnected, retrying connection");

            this.joinLeaveManager.submitJoinRequest(aliveNode).subscribe(status -> {
                if (status) nodeConnected(aliveNode);
            });
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
        LOGGER.info("distributed manager started");

        this.joinExecutor.scheduleAtFixedRate(() -> {
            synchronized (this) {
                if (clientState == ClientState.IDLE && !bootstrapNodes.isEmpty() && knownNodes.isEmpty()) {
                    LOGGER.warn("node is idle, retry join");

                    for (Node n : NodeHelper.getRandomNodes(this.bootstrapNodes)) {
                        try {
                            sendJoinRequest(n);
                        } catch (IOException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }
                } else {
                    LOGGER.info("join with cluster completed, shutting down join retry manager");

                    this.joinExecutor.shutdown();
                }
            }
        }, joinTimeout, joinTimeout, TimeUnit.SECONDS);

        LOGGER.info("join retry manager started");

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            synchronized (this) {
                this.knownNodes.forEach(n -> {
                    try {
                        sendNodeAliveGossip(myNode, n, 1);
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                });
            }
        }, 0, nodeGossipInterval, TimeUnit.SECONDS);

        LOGGER.info("node alive gossip sender started");

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            synchronized (this) {
                this.knownNodes.forEach(Node::reduceTTL);

                LOGGER.trace("node ttl cycle completed");
            }
        }, 1, 1, TimeUnit.SECONDS);

        LOGGER.info("node ttl reducer started");

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            synchronized (this) {
                List<Node> unresponsiveNodes = this.knownNodes.stream().filter(Node::isTTLExpired).collect(Collectors.toList());
                this.knownNodes.removeAll(unresponsiveNodes);

                if (!unresponsiveNodes.isEmpty()) {
                    this.knownNodesBehaviourSubject.onNext(this.knownNodes);
                }

                unresponsiveNodes.forEach(un -> {
                    try {
                        sendJoinRequest(un);
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }

                    this.knownNodes.forEach(n -> {
                        try {
                            sendNodeUnresponsiveGossip(un, n, 1);
                        } catch (IOException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    });
                });

                LOGGER.trace("node ttl scanned");
            }
        }, nodeTTL, nodeTTL, TimeUnit.SECONDS);

        LOGGER.info("node ttl scanner started");
    }

    @Override
    public void stopService() {
        this.scheduledExecutorService.shutdown();

        LOGGER.info("distributed manager stopped");
    }

    private RegisterResponse sendRegisterRequest() throws IOException {
        RegisterRequest registerRequest = new RegisterRequest(myNode, serviceName);

        LOGGER.info("send register request");

        String response = tcpMessageSender.sendMessage(registerRequest, bootstrapNode);

        RegisterResponse registerResponse = new RegisterResponse();
        registerResponse.deserialize(response);

        LOGGER.info("register response received: \"{}\"", registerResponse.getStatus().toString());

        return registerResponse;
    }

    private UnregisterResponse sendUnregisterRequest() throws IOException {
        UnregisterRequest unregisterRequest = new UnregisterRequest(myNode, serviceName);

        LOGGER.info("send unregister request");

        String response = tcpMessageSender.sendMessage(unregisterRequest, bootstrapNode);

        UnregisterResponse unregisterResponse = new UnregisterResponse();
        unregisterResponse.deserialize(response);

        LOGGER.info("unregister response received: \"{}\"", unregisterResponse.getStatus().toString());

        return unregisterResponse;
    }

    private void sendNodeDiscoveredGossip(Node discoveredNode, Node destinationNode, int hop) throws IOException {
        NodeDiscoveredGossip nodeDiscoveredGossip = new NodeDiscoveredGossip(discoveredNode, hop);

        if (discoveredNode.getId().equals(destinationNode.getId())) {
            LOGGER.warn("cannot send node: \"{}\" discovered gossip to same node: \"{}\"", discoveredNode.getId(), destinationNode.getId());
            return;
        } else if (nodeDiscoveredGossip.isMaxHopsReached(gossipMaxHops + 1)) {
            LOGGER.warn("gossip max hop count reached");
            return;
        }

        LOGGER.info("send node: \"{}\" discovered gossip to: \"{}\"", discoveredNode.getId(), destinationNode.getId());

        udpMessageSender.sendMessage(nodeDiscoveredGossip, destinationNode);
    }

    private void sendNodeUnresponsiveGossip(Node unresponsiveNode, Node destinationNode, int hop) throws IOException {
        NodeUnresponsiveGossip nodeUnresponsiveGossip = new NodeUnresponsiveGossip(unresponsiveNode, hop);

        if (unresponsiveNode.getId().equals(destinationNode.getId())) {
            LOGGER.warn("cannot send node: \"{}\" unresponsive gossip to same node: \"{}\"", unresponsiveNode.getId(), destinationNode.getId());
            return;
        } else if (nodeUnresponsiveGossip.isMaxHopsReached(gossipMaxHops + 1)) {
            LOGGER.warn("gossip max hop count reached");
            return;
        }

        LOGGER.info("send node: \"{}\" unresponsive gossip to: \"{}\"", unresponsiveNode.getId(), destinationNode.getId());

        udpMessageSender.sendMessage(nodeUnresponsiveGossip, destinationNode);
    }

    private void sendNodeAliveGossip(Node aliveNode, Node destinationNode, int hop) throws IOException {
        NodeAliveGossip nodeAliveGossip = new NodeAliveGossip(aliveNode, hop);

        if (aliveNode.getId().equals(destinationNode.getId())) {
            LOGGER.trace("cannot send node: \"{}\" node alive gossip to same node: \"{}\"", aliveNode.getId(), destinationNode.getId());
            return;
        } else if (nodeAliveGossip.isMaxHopsReached(gossipMaxHops + 1)) {
            LOGGER.trace("gossip max hop count reached");
            return;
        }

        LOGGER.trace("send heart beat message to: \"{}\"", destinationNode.getId());

        udpMessageSender.sendMessage(nodeAliveGossip, destinationNode);
    }

    private void sendJoinRequest(Node n) throws IOException {
        this.joinLeaveManager.submitJoinRequest(n).subscribe(status -> {
            if (status) nodeConnected(n);
        });
    }
}
