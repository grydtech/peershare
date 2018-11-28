package com.grydtech.peershare.web.controllers;

import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.distributed.models.search.FileSearchRequest;
import com.grydtech.peershare.distributed.models.search.FileSearchResponse;
import com.grydtech.peershare.distributed.services.ClusterManager;
import com.grydtech.peershare.distributed.services.FileSearchManager;
import com.grydtech.peershare.files.models.FileInfo;
import com.grydtech.peershare.report.services.Reporter;
import com.grydtech.peershare.web.models.*;
import com.grydtech.peershare.web.services.Downloader;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.UUID;

@Controller
public class WebSocketController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketController.class);

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final FileSearchManager fileSearchManager;
    private final ClusterManager clusterManager;
    private final Reporter reporter;
    private final Downloader downloader;
    private final Node myNode;

    @Autowired
    public WebSocketController(SimpMessagingTemplate simpMessagingTemplate, FileSearchManager fileSearchManager, ClusterManager clusterManager, Reporter reporter, Downloader downloader, Node myNode) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.fileSearchManager = fileSearchManager;
        this.clusterManager = clusterManager;
        this.reporter = reporter;
        this.downloader = downloader;
        this.myNode = myNode;
    }

    @MessageMapping("/search")
    public void search(SearchRequest searchRequest) throws IOException {
        LOGGER.info("file search request received searchText: \"{}\"", searchRequest.getSearchText());

        FileSearchRequest fileSearchRequest = new FileSearchRequest(myNode, searchRequest.getSearchText(), UUID.fromString(searchRequest.getSearchId()), 0);
        Observable<FileSearchResponse> resultObservable = fileSearchManager.submitFileSearchRequest(fileSearchRequest);

        reporter.reportSearchStarted(UUID.fromString(searchRequest.getSearchId()));

        resultObservable.subscribe(fileSearchResponse -> fileSearchResponse.getFileNames().forEach(fn -> {
            FileInfo fileInfo = new FileInfo(fn);
            SearchResult searchResult = new SearchResult(fileInfo.getId(), fileInfo.getName(), fileSearchResponse.getNode().getHost(), fileSearchResponse.getNode().getPort());
            SearchResponse searchResponse = new SearchResponse(searchRequest.getSearchId(), searchResult);
            simpMessagingTemplate.convertAndSend("/topic/results", searchResponse);
        }));
    }

    @MessageMapping("/info")
    public void info() {
        LOGGER.info("info request received");

        simpMessagingTemplate.convertAndSend("/topic/routing-table", new RoutingTableResponse(clusterManager.getConnectedCluster()));

        clusterManager.getConnectedClusterObservable().subscribe(nodes -> {
            simpMessagingTemplate.convertAndSend("/topic/routing-table", new RoutingTableResponse(nodes));
        });
    }

    @MessageMapping("/download")
    public void download(DownloadRequest downloadRequest) throws IOException {
        LOGGER.info("download request received");

        DownloadResponse downloadResponse = downloader.download(downloadRequest);
        simpMessagingTemplate.convertAndSend("/topic/download", downloadResponse);
    }
}
