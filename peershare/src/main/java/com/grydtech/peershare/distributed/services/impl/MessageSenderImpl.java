package com.grydtech.peershare.distributed.services.impl;

import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.distributed.models.bootstrap.*;
import com.grydtech.peershare.distributed.models.gossip.NodeDiscoveredGossip;
import com.grydtech.peershare.distributed.models.gossip.NodeUnresponsiveGossip;
import com.grydtech.peershare.distributed.models.hearbeat.HeartBeatMessage;
import com.grydtech.peershare.distributed.models.peer.*;
import com.grydtech.peershare.distributed.models.search.FileSearchRequest;
import com.grydtech.peershare.distributed.models.search.FileSearchResponse;
import com.grydtech.peershare.distributed.models.search.FileSearchResponseStatus;
import com.grydtech.peershare.distributed.services.FileSearchReporter;
import com.grydtech.peershare.distributed.services.MessageSender;
import com.grydtech.peershare.shared.services.TCPMessageSender;
import com.grydtech.peershare.shared.services.UDPMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MessageSenderImpl implements MessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSenderImpl.class);

    private final Map<String, Node> nodeMap = new HashMap<>();

    @Value("${server.name}")
    private String serviceName;

    @Value("${gossip.max-hops}")
    private int gossipMaxHops;

    @Value("${search.max-hops}")
    private int searchMaxHops;

    private final UDPMessageSender udpMessageSender;
    private final TCPMessageSender tcpMessageSender;
    private final Node myNode;
    private final Node bootstrapNode;

    @Autowired
    public MessageSenderImpl(UDPMessageSender udpMessageSender, TCPMessageSender tcpMessageSender, Node myNode, Node bootstrapNode) {
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

        nodeMap.put(peerJoinRequest.getMessageId().toString(), destinationNode);

        udpMessageSender.sendMessage(peerJoinRequest, destinationNode);
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
    }

    @Override
    public void sendLeaveResponse(Node destinationNode, UUID requestId) throws IOException {
        PeerLeaveResponse peerLeaveResponse = new PeerLeaveResponse(PeerJoinResponseStatus.SUCCESSFUL, requestId);

        LOGGER.info("send leave response to: \"{}\"", destinationNode.getId());

        udpMessageSender.sendMessage(peerLeaveResponse, destinationNode);
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
    }

    @Override
    public void sendFileSearchResponse(List<String> fileList, Node destinationNode, UUID requestId, int hops) throws IOException {
        FileSearchResponse fileSearchResponse = new FileSearchResponse(myNode, fileList, requestId, hops, FileSearchResponseStatus.fromCode(fileList.size()));

        LOGGER.info("send search response: \"{}\" to: \"{}\"", fileList.toString(), destinationNode.getId());

        udpMessageSender.sendMessage(fileSearchResponse, destinationNode);
    }

    @Override
    public void sendHeartBeatMessage(Node destinationNode) throws IOException {
        HeartBeatMessage heartBeatMessage = new HeartBeatMessage(myNode);

        LOGGER.trace("send heart beat message to: \"{}\"", destinationNode.getId());

        udpMessageSender.sendMessage(heartBeatMessage, destinationNode);
    }

    @Override
    public Node getDestinationNode(String messageId) {
        return nodeMap.get(messageId);
    }
}
