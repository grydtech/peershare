package com.grydtech.peershare.web.models;

public class DownloadResponse {

    private final String fileName;
    private final String filePath;
    private final String receivedHash;
    private final String generatedHash;
    private final long fileSize;
    private final DownloadStatus status;

    public DownloadResponse(String fileName, String filePath, String receivedHash, String generatedHash, long fileSize, DownloadStatus status) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.receivedHash = receivedHash;
        this.generatedHash = generatedHash;
        this.fileSize = fileSize;
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getReceivedHash() {
        return receivedHash;
    }

    public String getGeneratedHash() {
        return generatedHash;
    }

    public long getFileSize() {
        return fileSize;
    }

    public DownloadStatus getStatus() {
        return status;
    }
}
