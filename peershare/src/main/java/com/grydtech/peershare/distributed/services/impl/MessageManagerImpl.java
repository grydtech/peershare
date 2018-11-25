package com.grydtech.peershare.distributed.services.impl;

import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.distributed.models.bootstrap.*;
import com.grydtech.peershare.distributed.models.gossip.NodeDiscoveredGossip;
import com.grydtech.peershare.distributed.models.gossip.NodeUnresponsiveGossip;
import com.grydtech.peershare.distributed.models.gossip.NodeAliveGossip;
import com.grydtech.peershare.distributed.models.peer.*;
import com.grydtech.peershare.distributed.models.search.FileSearchRequest;
import com.grydtech.peershare.distributed.models.search.FileSearchResponse;
import com.grydtech.peershare.distributed.models.search.FileSearchResponseStatus;
import com.grydtech.peershare.distributed.services.MessageManager;
import com.grydtech.peershare.shared.models.Message;
import com.grydtech.peershare.shared.services.TCPMessageSender;
import com.grydtech.peershare.shared.services.UDPMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class MessageManagerImpl implements MessageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageManagerImpl.class);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Message> requestMap = new HashMap<>();
    private final Queue<String> requestQueue = new ConcurrentLinkedQueue<>();

    @Value("${server.name}")
    private String serviceName;

    @Value("${gossip.max-hops}")
    private int gossipMaxHops;

    @Value("${search.max-hops}")
    private int searchMaxHops;

    @Value("${request.timeout}")
    private int requestTimeout;

    private final UDPMessageSender udpMessageSender;
    private final TCPMessageSender tcpMessageSender;
    private final Node myNode;
    private final Node bootstrapNode;

    @Autowired
    public MessageManagerImpl(UDPMessageSender udpMessageSender, TCPMessageSender tcpMessageSender, Node myNode, Node bootstrapNode) {
        this.udpMessageSender = udpMessageSender;
        this.tcpMessageSender = tcpMessageSender;
        this.myNode = myNode;
        this.bootstrapNode = bootstrapNode;
    }

    @Override
    public RegisterResponse sendRegisterRequest() throws IOException {
        RegisterRequest registerRequest = new RegisterRequest(myNode, serviceName);

        LOGGER.info("send register request");

        String response = tcpMessageSender.sendMessage(registerRequest, bootstrapNode);

        RegisterResponse registerResponse = new RegisterResponse();
        registerResponse.deserialize(response);

        LOGGER.info("register response received: \"{}\"", registerResponse.getStatus().toString());

        return registerResponse;
    }

    @Override
    public UnregisterResponse sendUnregisterRequest() throws IOException {
        UnregisterRequest unregisterRequest = new UnregisterRequest(myNode, serviceName);

        LOGGER.info("send unregister request");

        String response = tcpMessageSender.sendMessage(unregisterRequest, bootstrapNode);

        UnregisterResponse unregisterResponse = new UnregisterResponse();
        unregisterResponse.deserialize(response);

        LOGGER.info("unregister response received: \"{}\"", unregisterResponse.getStatus().toString());

        return unregisterResponse;
    }

    @Override
    public void sendJoinRequest(Node destinationNode) throws IOException {
        PeerJoinRequest peerJoinRequest = new PeerJoinRequest(myNode);

        LOGGER.info("send join request to: \"{}\"", destinationNode.getId());

        udpMessageSender.sendMessage(peerJoinRequest, destinationNode);

        addRequestToQueue(peerJoinRequest);
    }

    @Override
    public void sendJoinResponse(Node destinationNode, UUID requestId) throws IOException {
        PeerJoinResponse peerJoinResponse = new PeerJoinResponse(PeerJoinResponseStatus.SUCCESSFUL, requestId);

        LOGGER.info("send join response to: \"{}\"", destinationNode.getId());

        udpMessageSender.sendMessage(peerJoinResponse, destinationNode);
    }

    @Override
    public void sendLeaveRequest(Node destinationNode) throws IOException {
        PeerLeaveRequest peerLeaveRequest = new PeerLeaveRequest(myNode);

        LOGGER.info("send leave request to: \"{}\"", destinationNode.getId());

        udpMessageSender.sendMessage(peerLeaveRequest, destinationNode);

        addRequestToQueue(peerLeaveRequest);
    }

    @Override
    public void sendLeaveResponse(Node destinationNode, UUID requestId) throws IOException {
        PeerLeaveResponse peerLeaveResponse = new PeerLeaveResponse(PeerJoinResponseStatus.SUCCESSFUL, requestId);

        LOGGER.info("send leave response to: \"{}\"", destinationNode.getId());

        udpMessageSender.sendMessage(peerLeaveResponse, destinationNode);
    }

    @Override
    public void sendFileSearchRequest(String keyword, Node startNode, Node destinationNode, UUID requestId, int hop) throws IOException {
        FileSearchRequest fileSearchRequest = new FileSearchRequest(startNode, keyword, requestId, hop);

        if (startNode.getId().equals(destinationNode.getId())) {
            LOGGER.warn("cannot send search request to same node");
            return;
        } else if (fileSearchRequest.isMaxHopsReached(gossipMaxHops + 1)) {
            LOGGER.warn("search max hop count reached");
            return;
        }

        LOGGER.info("send search request: \"{}\" to: \"{}\"", keyword, destinationNode.getId());

        udpMessageSender.sendMessage(fileSearchRequest, destinationNode);

        addRequestToQueue(fileSearchRequest);
    }

    @Override
    public void sendFileSearchResponse(List<String> fileList, Node destinationNode, UUID requestId, int hops) throws IOException {
        FileSearchResponse fileSearchResponse = new FileSearchResponse(myNode, fileList, requestId, hops, FileSearchResponseStatus.fromCode(fileList.size()));

        LOGGER.info("send search response: \"{}\" to: \"{}\"", fileList.toString(), destinationNode.getId());

        udpMessageSender.sendMessage(fileSearchResponse, destinationNode);
    }

    @Override
    public void sendNodeDiscoveredGossip(Node discoveredNode, Node destinationNode, int hop) throws IOException {
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

    @Override
    public void sendNodeUnresponsiveGossip(Node unresponsiveNode, Node destinationNode, int hop) throws IOException {
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

    @Override
    public void sendNodeAliveGossip(Node aliveNode, Node destinationNode, int hop) throws IOException {
        NodeAliveGossip nodeAliveGossip = new NodeAliveGossip(aliveNode, hop);

        if (aliveNode.getId().equals(destinationNode.getId())) {
            LOGGER.warn("cannot send node: \"{}\" node alive gossip to same node: \"{}\"", aliveNode.getId(), destinationNode.getId());
            return;
        } else if (nodeAliveGossip.isMaxHopsReached(gossipMaxHops + 1)) {
            LOGGER.warn("gossip max hop count reached");
            return;
        }

        LOGGER.trace("send heart beat message to: \"{}\"", destinationNode.getId());

        udpMessageSender.sendMessage(nodeAliveGossip, destinationNode);
    }

    @Override
    public Message getSentMessageById(String messageId) {
        return requestMap.get(messageId);
    }

    private synchronized void addRequestToQueue(Message message) {
        requestMap.put(message.getMessageId().toString(), message);
        requestQueue.add(message.getMessageId().toString());
    }

    @Override
    public void startService() {
        LOGGER.info("message manager started");

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            synchronized (this) {
                String key = requestQueue.remove();
                requestMap.remove(key);

                LOGGER.trace("request: \"{}\" cleaned up", key);
            }
        }, requestTimeout, requestTimeout, TimeUnit.SECONDS);

        LOGGER.info("request cleanup started");
    }

    @Override
    public void stopService() {
        this.requestMap.clear();
        this.requestQueue.clear();

        this.scheduledExecutorService.shutdown();

        LOGGER.info("message manager stopped");
    }
}
