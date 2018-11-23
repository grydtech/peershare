package com.grydtech.peershare.client.services;

import com.grydtech.peershare.client.models.Node;
import com.grydtech.peershare.client.models.search.FileSearchResult;
import com.grydtech.peershare.shared.services.Manager;
import io.reactivex.Observable;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface FileSearchManager extends Manager {

    Observable<FileSearchResult> submitSearch(UUID searchId, String keyword) throws IOException;

    void submitSearchResult(UUID searchId, List<String> discoveredFiles, Node node, int hops);

    void acceptSearchRequest(UUID searchId, String keyWord, Node startNode, int hop) throws IOException;
}
