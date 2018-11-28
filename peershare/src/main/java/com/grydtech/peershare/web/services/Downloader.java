package com.grydtech.peershare.web.services;

import com.grydtech.peershare.web.models.DownloadRequest;
import com.grydtech.peershare.web.models.DownloadResponse;

import java.io.IOException;

public interface Downloader {
    DownloadResponse download(DownloadRequest downloadRequest) throws IOException;
}
