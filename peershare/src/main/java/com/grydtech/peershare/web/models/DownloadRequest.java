package com.grydtech.peershare.web.models;

public class DownloadRequest {

    private final String fileName;
    private final String fileUrl;

    public DownloadRequest(String fileName, String fileUrl) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }
}
