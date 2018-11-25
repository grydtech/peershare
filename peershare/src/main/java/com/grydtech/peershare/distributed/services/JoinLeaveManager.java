package com.grydtech.peershare.distributed.services;

import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.distributed.models.peer.PeerResponseStatus;
import com.grydtech.peershare.shared.services.Manager;
import io.reactivex.Observable;

import java.io.IOException;
import java.util.UUID;

public interface JoinLeaveManager extends Manager {

    Observable<Boolean> submitJoinRequest(Node destinationNode) throws IOException;

    void acceptJoinRequest(UUID requestId, Node node) throws IOException;

    Observable<Boolean> submitLeaveRequest(Node destinationNode) throws IOException;

    void acceptLeaveRequest(UUID requestId, Node node) throws IOException;

    void submitResponse(UUID requestId, PeerResponseStatus status);
}
