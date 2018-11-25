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

    private final Map<String, BehaviorSubject<Boolean>> requestMap = new HashMap<>();
    private final List<MessageInfo> messages = new ArrayList<>();

    @Value("${join.timeout}")
    private int joinTimeout;

    private final UDPMessageSender udpMessageSender;
    private final Node myNode;

    @Autowired
    public JoinLeaveManagerImpl(UDPMessageSender udpMessageSender, Node myNode) {
        this.udpMessageSender = udpMessageSender;
        this.myNode = myNode;
    }

    @Override
    public Observable<Boolean> submitJoinRequest(Node destinationNode) throws IOException {
        LOGGER.info("join request submitted");

        BehaviorSubject<Boolean> behaviorSubject = BehaviorSubject.create();
        PeerJoinRequest peerJoinRequest = new PeerJoinRequest(myNode);

        synchronized (this) {
            LOGGER.trace("add request; \"{}\" to queue for cleanup", peerJoinRequest.getMessageId());

            messages.add(new MessageInfo(peerJoinRequest.getMessageId()));
            requestMap.put(peerJoinRequest.getMessageId().toString(), behaviorSubject);

            LOGGER.info("send join request to: \"{}\"", destinationNode.getId());

            udpMessageSender.sendMessage(peerJoinRequest, destinationNode);
        }

        return behaviorSubject;
    }

    @Override
    public void acceptJoinRequest(UUID requestId, Node destinationNode) throws IOException {
        PeerJoinResponse peerJoinResponse = new PeerJoinResponse(PeerResponseStatus.SUCCESSFUL, requestId);

        LOGGER.info("send join response to: \"{}\"", destinationNode.getId());

        udpMessageSender.sendMessage(peerJoinResponse, destinationNode);
    }

    @Override
    public Observable<Boolean> submitLeaveRequest(Node destinationNode) throws IOException {
        LOGGER.info("leave request submitted");

        BehaviorSubject<Boolean> behaviorSubject = BehaviorSubject.create();
        PeerLeaveRequest peerLeaveRequest = new PeerLeaveRequest(myNode);

        synchronized (this) {
            LOGGER.trace("add request; \"{}\" to queue for cleanup", peerLeaveRequest.getMessageId());

            messages.add(new MessageInfo(peerLeaveRequest.getMessageId()));
            requestMap.put(peerLeaveRequest.getMessageId().toString(), behaviorSubject);

            LOGGER.info("send leave request to: \"{}\"", destinationNode.getId());

            udpMessageSender.sendMessage(peerLeaveRequest, destinationNode);
        }

        return behaviorSubject;
    }

    @Override
    public void acceptLeaveRequest(UUID requestId, Node destinationNode) throws IOException {
        PeerLeaveResponse peerLeaveResponse = new PeerLeaveResponse(PeerResponseStatus.SUCCESSFUL, requestId);

        LOGGER.info("send leave response to: \"{}\"", destinationNode.getId());

        udpMessageSender.sendMessage(peerLeaveResponse, destinationNode);
    }

    @Override
    public synchronized void submitResponse(UUID requestId, PeerResponseStatus status) {
        BehaviorSubject<Boolean> behaviorSubject = requestMap.get(requestId.toString());

        if (behaviorSubject == null) {
            LOGGER.warn("request: \"{}\" timed out", requestId.toString());
            return;
        }

        LOGGER.trace("emit received response via behaviour subject");

        if (status == PeerResponseStatus.SUCCESSFUL) {
            behaviorSubject.onNext(true);
        } else {
            behaviorSubject.onNext(false);
        }

        requestMap.remove(requestId.toString());
        messages.removeIf(messageInfo -> messageInfo.getMessageId().toString().equals(requestId.toString()));
    }

    @Override
    public void startService() {
        LOGGER.info("join leave manager started");

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            synchronized (this) {
                List<MessageInfo> expiredMessages = messages.stream().filter(m -> m.isExpired(joinTimeout)).collect(Collectors.toList());

                expiredMessages.forEach(em -> {
                    String key = em.getMessageId().toString();
                    BehaviorSubject<Boolean> behaviorSubject = requestMap.get(key);
                    requestMap.remove(key);
                    behaviorSubject.onComplete();

                    LOGGER.trace("join leave request: \"{}\" send completed response and remove", key);
                });

                messages.removeAll(expiredMessages);
            }
        }, joinTimeout, joinTimeout, TimeUnit.SECONDS);

        LOGGER.info("join leave request cleanup started");
    }

    @Override
    public void stopService() {
        this.requestMap.forEach((key, value) -> {
            value.onComplete();
        });

        this.requestMap.clear();
        this.messages.clear();

        this.scheduledExecutorService.shutdown();

        LOGGER.info("join leave manager stopped");
    }
}
