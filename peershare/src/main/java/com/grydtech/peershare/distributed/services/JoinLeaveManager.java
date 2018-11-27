package com.grydtech.peershare.distributed.services;

import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.distributed.models.peer.*;
import com.grydtech.peershare.shared.services.Manager;
import io.reactivex.Observable;

import java.io.IOException;

public interface JoinLeaveManager extends Manager {

    Observable<PeerResponseStatus> submitJoinRequest(PeerJoinRequest peerJoinRequest, Node destinationNode) throws IOException;

    void handleJoinRequest(PeerJoinRequest peerJoinRequest) throws IOException;

    void handleJoinResponse(PeerJoinResponse peerJoinResponse);

    Observable<PeerResponseStatus> submitLeaveRequest(PeerLeaveRequest peerLeaveRequest, Node destinationNode) throws IOException;

    void handleLeaveRequest(PeerLeaveRequest peerLeaveRequest) throws IOException;

    void handleLeaveResponse(PeerLeaveResponse peerLeaveResponse);
}
