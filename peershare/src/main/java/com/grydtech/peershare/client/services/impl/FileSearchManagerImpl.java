package com.grydtech.peershare.client.services.impl;

import com.grydtech.peershare.client.models.Node;
import com.grydtech.peershare.client.models.search.FileSearchResult;
import com.grydtech.peershare.client.services.ClusterManager;
import com.grydtech.peershare.client.services.MessageSender;
import com.grydtech.peershare.client.services.FileSearchManager;
import com.grydtech.peershare.files.models.FileInfo;
import com.grydtech.peershare.files.services.FileStoreManager;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class FileSearchManagerImpl implements FileSearchManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSearchManagerImpl.class);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, BehaviorSubject<FileSearchResult>> resultsMap = new HashMap<>();
    private final Queue<String> searchQueue = new ConcurrentLinkedQueue<>();

    @Value("${search.timeout}")
    private int searchTimeout;

    private final Node myNode;
    private final FileStoreManager fileStoreManager;
    private final ClusterManager clusterManager;
    private final MessageSender messageSender;

    @Autowired
    public FileSearchManagerImpl(Node myNode, FileStoreManager fileStoreManager, ClusterManager clusterManager, MessageSender messageSender) {
        this.myNode = myNode;
        this.fileStoreManager = fileStoreManager;
        this.clusterManager = clusterManager;
        this.messageSender = messageSender;
    }

    @Override
    public Observable<FileSearchResult> submitSearch(UUID searchId, String keyword) throws IOException {
        BehaviorSubject<FileSearchResult> behaviorSubject = BehaviorSubject.create();
        List<FileInfo> myFiles = fileStoreManager.search(keyword);

        LOGGER.info("file search request submitted: \"{}\"", keyword);

        synchronized (this) {
            LOGGER.trace("add search; \"{}\" to queue for cleanup", searchId);

            searchQueue.add(searchId.toString());

            FileSearchResult fileSearchResult = new FileSearchResult(myNode, myFiles, 0);
            behaviorSubject.onNext(fileSearchResult);

            resultsMap.put(searchId.toString(), behaviorSubject);
        }

        LOGGER.info("send file search request to known nodes");

        for (Node n : clusterManager.getConnectedCluster()) {
            messageSender.sendFileSearchRequest(keyword, myNode, n, searchId, 1);
        }

        return behaviorSubject;
    }

    @Override
    public void submitSearchResult(UUID searchId, List<String> discoveredFiles, Node node, int hops) {
        synchronized (this) {
            BehaviorSubject<FileSearchResult> behaviorSubject = resultsMap.get(searchId.toString());

            if (behaviorSubject != null) {
                List<FileInfo> files = discoveredFiles.stream().map(FileInfo::new).collect(Collectors.toList());
                FileSearchResult fileSearchResult = new FileSearchResult(node, files, hops);

                LOGGER.info("push received search results");

                behaviorSubject.onNext(fileSearchResult);
            } else {
                LOGGER.warn("search already completed");
            }
        }
    }

    @Override
    public void acceptSearchRequest(UUID searchId, String keyWord, Node startNode, int hop) throws IOException {
        List<String> fileNames = fileStoreManager.search(keyWord).stream().map(FileInfo::getName).collect(Collectors.toList());

        messageSender.sendFileSearchResponse(fileNames, startNode, searchId, hop);

        LOGGER.info("send file search request to random nodes");

        for (Node n : clusterManager.getConnectedCluster()) {
            messageSender.sendFileSearchRequest(keyWord, startNode, n, searchId, hop + 1);
        }
    }

    @Override
    public void startService() {
        LOGGER.info("file search manager started");

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            synchronized (this) {
                String key = searchQueue.remove();
                BehaviorSubject<FileSearchResult> behaviorSubject = resultsMap.get(key);
                resultsMap.remove(key);
                behaviorSubject.onComplete();

                LOGGER.trace("search: \"{}\" send completed response and remove", key);
            }
        }, searchTimeout, searchTimeout, TimeUnit.SECONDS);

        LOGGER.info("search cleanup started");
    }

    @Override
    public void stopService() {
        this.resultsMap.clear();

        LOGGER.info("file search manager stopped");
    }
}
