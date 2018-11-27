package com.grydtech.peershare.distributed.services.impl;

import com.grydtech.peershare.distributed.models.MessageInfo;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.distributed.models.search.FileSearchRequest;
import com.grydtech.peershare.distributed.models.search.FileSearchResponse;
import com.grydtech.peershare.distributed.models.search.FileSearchResponseStatus;
import com.grydtech.peershare.distributed.services.ClusterManager;
import com.grydtech.peershare.distributed.services.FileSearchManager;
import com.grydtech.peershare.files.models.FileInfo;
import com.grydtech.peershare.files.services.FileStore;
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
public class FileSearchManagerImpl implements FileSearchManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSearchManagerImpl.class);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private final Map<String, BehaviorSubject<FileSearchResponse>> searchMap = new HashMap<>();
    private final List<MessageInfo> messages = new ArrayList<>();

    @Value("${search.timeout}")
    private int searchTimeout;

    @Value("${search.max-hops}")
    private int searchMaxHops;

    private final Node myNode;
    private final FileStore fileStore;
    private final ClusterManager clusterManager;
    private final UDPMessageSender udpMessageSender;

    @Autowired
    public FileSearchManagerImpl(Node myNode, FileStore fileStore, ClusterManager clusterManager, UDPMessageSender udpMessageSender) {
        this.myNode = myNode;
        this.fileStore = fileStore;
        this.clusterManager = clusterManager;
        this.udpMessageSender = udpMessageSender;
    }

    @Override
    public void startService() {
        LOGGER.info("file search manager started");

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            synchronized (this) {
                List<MessageInfo> expiredMessages = messages.stream().filter(m -> m.isExpired(searchTimeout)).collect(Collectors.toList());

                expiredMessages.forEach(em -> {
                    String key = em.getMessageId().toString();
                    BehaviorSubject<FileSearchResponse> behaviorSubject = searchMap.get(key);
                    searchMap.remove(key);
                    behaviorSubject.onComplete();

                    LOGGER.trace("search: \"{}\" send completed response and remove", key);
                });

                messages.removeAll(expiredMessages);
            }
        }, searchTimeout, searchTimeout, TimeUnit.SECONDS);

        LOGGER.info("search cleanup started");
    }

    @Override
    public void stopService() {
        this.searchMap.forEach((key, value) -> {
            value.onComplete();
        });

        this.searchMap.clear();
        this.messages.clear();

        this.scheduledExecutorService.shutdown();

        LOGGER.info("file search manager stopped");
    }

    @Override
    public Observable<FileSearchResponse> submitFileSearchRequest(FileSearchRequest fileSearchRequest) throws IOException {
        BehaviorSubject<FileSearchResponse> behaviorSubject = BehaviorSubject.create();
        List<String> fileList = fileStore.search(fileSearchRequest.getKeyword()).stream().map(FileInfo::getName).collect(Collectors.toList());

        LOGGER.info("file search request submitted: \"{}\"", fileSearchRequest.getKeyword());

        synchronized (this) {
            LOGGER.trace("add search; \"{}\" to queue for cleanup", fileSearchRequest.getMessageId().toString());

            messages.add(new MessageInfo(fileSearchRequest.getMessageId()));

            FileSearchResponse fileSearchResponse = new FileSearchResponse(myNode, fileList, fileSearchRequest.getMessageId(), 0, FileSearchResponseStatus.fromCode(fileList.size()));
            behaviorSubject.onNext(fileSearchResponse);

            searchMap.put(fileSearchRequest.getMessageId().toString(), behaviorSubject);
        }

        LOGGER.info("send file search request to known nodes");

        for (Node n : clusterManager.getConnectedCluster()) {
            sendFileSearchRequest(fileSearchRequest.getKeyword(), myNode, n, fileSearchRequest.getMessageId(), 1);
        }

        return behaviorSubject;
    }

    @Override
    public void handleFileSearchRequest(FileSearchRequest fileSearchRequest) throws IOException {
        List<String> fileNames = fileStore.search(fileSearchRequest.getKeyword()).stream().map(FileInfo::getName).collect(Collectors.toList());

        sendFileSearchResponse(fileNames, fileSearchRequest.getNode(), fileSearchRequest.getMessageId(), fileSearchRequest.getHop());

        LOGGER.info("send file search request to random nodes");

        for (Node n : clusterManager.getConnectedCluster()) {
            sendFileSearchRequest(fileSearchRequest.getKeyword(), fileSearchRequest.getNode(), n, fileSearchRequest.getMessageId(), fileSearchRequest.getHop() + 1);
        }
    }

    @Override
    public void handleFileSearchResponse(FileSearchResponse fileSearchResponse) {
        synchronized (this) {
            BehaviorSubject<FileSearchResponse> behaviorSubject = searchMap.get(fileSearchResponse.getMessageId().toString());

            if (behaviorSubject != null) {
                LOGGER.info("push received search results");

                behaviorSubject.onNext(fileSearchResponse);
            } else {
                LOGGER.warn("search already completed");
            }
        }
    }

    private void sendFileSearchRequest(String keyword, Node startNode, Node destinationNode, UUID requestId, int hop) throws IOException {
        FileSearchRequest fileSearchRequest = new FileSearchRequest(startNode, keyword, requestId, hop);

        if (startNode.getId().equals(destinationNode.getId())) {
            LOGGER.warn("cannot send search request to same node");
            return;
        } else if (fileSearchRequest.isMaxHopsReached(searchMaxHops + 1)) {
            LOGGER.warn("search max hop count reached");
            return;
        }

        LOGGER.info("send search request: \"{}\" to: \"{}\"", keyword, destinationNode.getId());

        udpMessageSender.sendMessage(fileSearchRequest, destinationNode);
    }

    private void sendFileSearchResponse(List<String> fileList, Node destinationNode, UUID requestId, int hops) throws IOException {
        FileSearchResponse fileSearchResponse = new FileSearchResponse(myNode, fileList, requestId, hops, FileSearchResponseStatus.fromCode(fileList.size()));

        LOGGER.info("send search response: \"{}\" to: \"{}\"", fileList.toString(), destinationNode.getId());

        udpMessageSender.sendMessage(fileSearchResponse, destinationNode);
    }
}
