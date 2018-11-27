package com.grydtech.peershare.distributed.services;

import com.grydtech.peershare.distributed.models.search.FileSearchRequest;
import com.grydtech.peershare.distributed.models.search.FileSearchResponse;
import com.grydtech.peershare.shared.services.Manager;
import io.reactivex.Observable;

import java.io.IOException;

public interface FileSearchManager extends Manager {

    Observable<FileSearchResponse> submitFileSearchRequest(FileSearchRequest fileSearchRequest) throws IOException;

    void handleFileSearchRequest(FileSearchRequest fileSearchRequest) throws IOException;

    void handleFileSearchResponse(FileSearchResponse fileSearchResponse);
}
