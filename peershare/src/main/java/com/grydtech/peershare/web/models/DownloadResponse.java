package com.grydtech.peershare.web.models;

public class DownloadResponse {

    private final String fileName;
    private final String filePath;
    private final DownloadStatus status;

    public DownloadResponse(String fileName, String filePath, DownloadStatus status) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public DownloadStatus getStatus() {
        return status;
    }
}
