package com.grydtech.peershare.client.services.impl;

import com.grydtech.peershare.client.exceptions.BootstrapException;
import com.grydtech.peershare.client.exceptions.IllegalCommandException;
import com.grydtech.peershare.client.helpers.NodeHelper;
import com.grydtech.peershare.client.models.ClientState;
import com.grydtech.peershare.client.models.Node;
import com.grydtech.peershare.client.models.bootstrap.RegisterResponse;
import com.grydtech.peershare.client.services.ClusterManager;
import com.grydtech.peershare.client.services.MessageSender;
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

    @Value("${node.heart-beat-interval}")
    private int nodeHeartBeatInterval;

    @Value("${join.retry-interval}")
    private int joinRetryInterval;

    private final MessageSender messageSender;
    private ClientState clientState = ClientState.DISCONNECTED;

    @Autowired
    public ClusterManagerImpl(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public synchronized void register() throws IOException, BootstrapException {
        RegisterResponse registerResponse = this.messageSender.sendRegisterRequest();

        switch (registerResponse.getStatus()) {
            case ERROR:
                throw new BootstrapException("invalid command, please check again");
            case ALREADY_REGISTERED:
                throw new BootstrapException("already registered");
            case BOOTSTRAP_SERVER_FULL:
                throw new BootstrapException("bootstrap server full");
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

        this.messageSender.sendUnregisterRequest();

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
            this.messageSender.sendJoinRequest(n);
        }
    }

    @Override
    public synchronized void leave() throws IOException {
        for (Node n : this.knownNodes) {
            this.messageSender.sendLeaveRequest(n);
        }
        this.knownNodes.clear();
        this.knownNodesBehaviourSubject.onNext(this.knownNodes);

        LOGGER.info("node disconnected from cluster");

        this.clientState = ClientState.DISCONNECTED;
    }

    @Override
    public synchronized void nodeDiscovered(Node discoveredNode, int hop) throws IOException {
        Optional<Node> node = this.knownNodes.stream().filter(n -> n.getId().equals(discoveredNode.getId())).findFirst();

        if (!node.isPresent()) {
            LOGGER.info("send join request to: \"{}\"", discoveredNode.getId());

            messageSender.sendJoinRequest(discoveredNode);

            LOGGER.info("select random nodes to send node discovered gossip");

            for (Node n : NodeHelper.getRandomNodes(this.knownNodes)) {
                messageSender.sendNodeDiscoveredGossip(discoveredNode, n, hop);
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

            LOGGER.info("select random nodes to send node unresponsive gossip");

            for (Node n : NodeHelper.getRandomNodes(this.knownNodes)) {
                messageSender.sendNodeUnresponsiveGossip(unresponsiveNode, n, hop);
            }
        }
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
                    messageSender.sendNodeDiscoveredGossip(connectedNode, n, 1);
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
    public synchronized void nodeAlive(Node aliveNode) throws IOException {
        Optional<Node> node = this.knownNodes.stream().filter(n -> n.getId().equals(aliveNode.getId())).findFirst();

        if (node.isPresent()) {
            LOGGER.trace("node: \"{}\" ttl reset", aliveNode.getId());

            node.get().resetTTL();
        } else {
            LOGGER.warn("node disconnected, retrying connection");

            this.messageSender.sendJoinRequest(aliveNode);
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
        LOGGER.info("client manager started");

        this.joinExecutor.scheduleAtFixedRate(() -> {
            synchronized (this) {
                if (clientState == ClientState.IDLE && !bootstrapNodes.isEmpty() && knownNodes.isEmpty()) {
                    LOGGER.warn("node is idle, retry join");

                    for (Node n : NodeHelper.getRandomNodes(this.bootstrapNodes)) {
                        try {
                            this.messageSender.sendJoinRequest(n);
                        } catch (IOException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }
                } else {
                    LOGGER.info("join with cluster completed, shutting down join retry manager");

                    this.joinExecutor.shutdown();
                }
            }
        }, joinRetryInterval, joinRetryInterval, TimeUnit.SECONDS);

        LOGGER.info("join retry manager started");

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            synchronized (this) {
                this.knownNodes.forEach(n -> {
                    try {
                        messageSender.sendHeartBeatMessage(n);
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                });
            }
        }, 0, nodeHeartBeatInterval, TimeUnit.SECONDS);

        LOGGER.info("hear beat sender started");

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
                        this.messageSender.sendJoinRequest(un);
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }

                    this.knownNodes.forEach(n -> {
                        try {
                            messageSender.sendNodeUnresponsiveGossip(un, n, 1);
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

        LOGGER.info("client manager stopped");
    }
}
