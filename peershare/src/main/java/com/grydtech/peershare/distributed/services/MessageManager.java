package com.grydtech.peershare.distributed.services;

import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.distributed.models.bootstrap.RegisterResponse;
import com.grydtech.peershare.distributed.models.bootstrap.UnregisterResponse;
import com.grydtech.peershare.shared.models.Message;
import com.grydtech.peershare.shared.services.Manager;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface MessageManager extends Manager {

    RegisterResponse sendRegisterRequest() throws IOException;

    UnregisterResponse sendUnregisterRequest() throws IOException;

    void sendJoinRequest(Node destinationNode) throws IOException;

    void sendJoinResponse(Node destinationNode, UUID requestId) throws IOException;

    void sendLeaveRequest(Node destinationNode) throws IOException;

    void sendLeaveResponse(Node destinationNode, UUID requestId) throws IOException;

    void sendFileSearchRequest(String keyword, Node startNode, Node destinationNode, UUID requestId, int hop) throws IOException;

    void sendFileSearchResponse(List<String> fileList, Node destinationNode, UUID requestId, int hops) throws IOException;

    void sendNodeDiscoveredGossip(Node discoveredNode, Node destinationNode, int hop) throws IOException;

    void sendNodeUnresponsiveGossip(Node unresponsiveNode, Node destinationNode, int hop) throws IOException;

    void sendNodeAliveGossip(Node node, Node destinationNode, int hops) throws IOException;

    Message getSentMessageById(String messageId);
}
