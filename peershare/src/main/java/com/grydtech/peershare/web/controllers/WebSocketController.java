package com.grydtech.peershare.web.controllers;

import com.grydtech.peershare.client.models.search.FileSearchResult;
import com.grydtech.peershare.client.services.FileSearchManager;
import com.grydtech.peershare.web.models.SearchRequest;
import com.grydtech.peershare.web.models.SearchResponse;
import com.grydtech.peershare.web.models.SearchResult;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.UUID;

@Controller
public class WebSocketController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketController.class);

    @Value("${search.results}")
    private String searchResults;

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final FileSearchManager fileSearchManager;

    @Autowired
    public WebSocketController(SimpMessagingTemplate simpMessagingTemplate, FileSearchManager fileSearchManager) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.fileSearchManager = fileSearchManager;
    }

    @MessageMapping("/search")
    public void search(SearchRequest searchRequest) throws IOException {
        LOGGER.info("file search request received searchText: \"{}\"", searchRequest.getSearchText());

        Observable<FileSearchResult> resultObservable = fileSearchManager.submitSearch(UUID.fromString(searchRequest.getSearchId()), searchRequest.getSearchText());

        resultObservable.subscribe(fileSearchResult -> fileSearchResult.getFiles().forEach(f -> {
            SearchResult searchResult = new SearchResult(f.getId(), f.getName(), fileSearchResult.getNode().getHost(), fileSearchResult.getNode().getPort());
            SearchResponse searchResponse = new SearchResponse(searchRequest.getSearchId(), searchResult);
            simpMessagingTemplate.convertAndSend(searchResults, searchResponse);
        }));
    }
}
