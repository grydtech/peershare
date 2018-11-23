package com.grydtech.peershare.client.services;

import com.grydtech.peershare.client.models.Node;
import com.grydtech.peershare.client.models.bootstrap.RegisterResponse;
import com.grydtech.peershare.client.models.bootstrap.UnregisterResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface MessageSender {

    RegisterResponse sendRegisterRequest() throws IOException;

    UnregisterResponse sendUnregisterRequest() throws IOException;

    void sendJoinRequest(Node destinationNode) throws IOException;

    void sendJoinResponse(Node destinationNode, UUID requestId) throws IOException;

    void sendNodeDiscoveredGossip(Node discoveredNode, Node destinationNode, int hop) throws IOException;

    void sendNodeUnresponsiveGossip(Node unresponsiveNode, Node destinationNode, int hop) throws IOException;

    void sendFileSearchRequest(String keyword, Node startNode, Node destinationNode, UUID requestId, int hop) throws IOException;

    void sendFileSearchResponse(List<String> fileList, Node destinationNode, UUID requestId, int hops) throws IOException;

    void sendHeartBeatMessage(Node destinationNode) throws IOException;

    Node getDestinationNode(String messageId);
}
