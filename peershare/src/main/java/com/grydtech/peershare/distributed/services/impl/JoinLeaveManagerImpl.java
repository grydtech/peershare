package com.grydtech.peershare.distributed.services.impl;

import com.grydtech.peershare.distributed.models.MessageInfo;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.distributed.models.peer.*;
import com.grydtech.peershare.distributed.services.JoinLeaveManager;
import com.grydtech.peershare.shared.services.UDPMessageSender;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class JoinLeaveManagerImpl implements JoinLeaveManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JoinLeaveManagerImpl.class);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private final Map<String, BehaviorSubject<PeerResponseStatus>> requestMap = new HashMap<>();
    private final List<MessageInfo> messages = new ArrayList<>();

    @Value("${join.timeout}")
    private int joinTimeout;

    private final UDPMessageSender udpMessageSender;

    @Autowired
    public JoinLeaveManagerImpl(UDPMessageSender udpMessageSender) {
        this.udpMessageSender = udpMessageSender;
    }

    @Override
    public Observable<PeerResponseStatus> submitJoinRequest(PeerJoinRequest peerJoinRequest, Node destinationNode) throws IOException {
        LOGGER.info("DISTRIBUTED: join request submitted");
        BehaviorSubject<PeerResponseStatus> behaviorSubject = BehaviorSubject.create();

        synchronized (this) {
            LOGGER.trace("DISTRIBUTED: add request; \"{}\" to queue for cleanup", peerJoinRequest.getMessageId());

            messages.add(new MessageInfo(peerJoinRequest.getMessageId()));
            requestMap.put(peerJoinRequest.getMessageId().toString(), behaviorSubject);

            LOGGER.info("DISTRIBUTED: send join request to: \"{}\"", destinationNode.getId());

            udpMessageSender.sendMessage(peerJoinRequest, destinationNode);
        }

        return behaviorSubject;
    }

    @Override
    public void handleJoinRequest(PeerJoinRequest peerJoinRequest) throws IOException {
        PeerLeaveResponse peerLeaveResponse = new PeerLeaveResponse(PeerResponseStatus.SUCCESSFUL, peerJoinRequest.getMessageId());

        LOGGER.info("DISTRIBUTED: send leave response to: \"{}\"", peerJoinRequest.getNode().getId());

        udpMessageSender.sendMessage(peerLeaveResponse, peerJoinRequest.getNode());
    }

    @Override
    public void handleJoinResponse(PeerJoinResponse peerJoinResponse) {
        handleResponse(peerJoinResponse.getMessageId(), peerJoinResponse.getStatus());
    }

    @Override
    public Observable<PeerResponseStatus> submitLeaveRequest(PeerLeaveRequest peerLeaveRequest, Node destinationNode) throws IOException {
        LOGGER.info("DISTRIBUTED: leave request submitted");

        BehaviorSubject<PeerResponseStatus> behaviorSubject = BehaviorSubject.create();

        synchronized (this) {
            LOGGER.trace("DISTRIBUTED: add request; \"{}\" to queue for cleanup", peerLeaveRequest.getMessageId());

            messages.add(new MessageInfo(peerLeaveRequest.getMessageId()));
            requestMap.put(peerLeaveRequest.getMessageId().toString(), behaviorSubject);

            LOGGER.info("sDISTRIBUTED: end leave request to: \"{}\"", destinationNode.getId());

            udpMessageSender.sendMessage(peerLeaveRequest, destinationNode);
        }

        return behaviorSubject;
    }

    @Override
    public void handleLeaveRequest(PeerLeaveRequest peerLeaveRequest) throws IOException {
        PeerLeaveResponse peerLeaveResponse = new PeerLeaveResponse(PeerResponseStatus.SUCCESSFUL, peerLeaveRequest.getMessageId());

        LOGGER.info("DISTRIBUTED: send leave response to: \"{}\"", peerLeaveRequest.getNode().getId());

        udpMessageSender.sendMessage(peerLeaveResponse, peerLeaveRequest.getNode());
    }

    @Override
    public void handleLeaveResponse(PeerLeaveResponse peerLeaveResponse) {
        handleResponse(peerLeaveResponse.getMessageId(), peerLeaveResponse.getStatus());
    }

    @Override
    public void startService() {
        LOGGER.info("DISTRIBUTED: join leave manager started");

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            synchronized (this) {
                List<MessageInfo> expiredMessages = messages.stream().filter(m -> m.isExpired(joinTimeout)).collect(Collectors.toList());

                expiredMessages.forEach(em -> {
                    String key = em.getMessageId().toString();
                    BehaviorSubject<PeerResponseStatus> behaviorSubject = requestMap.get(key);
                    requestMap.remove(key);
                    behaviorSubject.onComplete();

                    LOGGER.trace("DISTRIBUTED: join leave request: \"{}\" send completed response and remove", key);
                });

                messages.removeAll(expiredMessages);
            }
        }, joinTimeout, joinTimeout, TimeUnit.SECONDS);

        LOGGER.info("DISTRIBUTED: join leave request cleanup started");
    }

    @Override
    public void stopService() {
        this.requestMap.forEach((key, value) -> {
            value.onComplete();
        });

        this.requestMap.clear();
        this.messages.clear();

        this.scheduledExecutorService.shutdown();

        LOGGER.info("DISTRIBUTED: join leave manager stopped");
    }

    private synchronized void handleResponse(UUID requestId, PeerResponseStatus status) {
        BehaviorSubject<PeerResponseStatus> behaviorSubject = requestMap.get(requestId.toString());

        if (behaviorSubject == null) {
            LOGGER.warn("DISTRIBUTED: request: \"{}\" timed out", requestId.toString());
            return;
        }

        LOGGER.trace("DISTRIBUTED: push received response");

        behaviorSubject.onNext(status);

        requestMap.remove(requestId.toString());
        messages.removeIf(messageInfo -> messageInfo.getMessageId().toString().equals(requestId.toString()));
    }
}
