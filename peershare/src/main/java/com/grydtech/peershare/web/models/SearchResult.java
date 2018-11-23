package com.grydtech.peershare.web.models;

public class SearchResult {

    private String fileId;
    private String fileName;
    private String host;
    private int port;

    public SearchResult(String fileId, String fileName, String host, int port) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.host = host;
        this.port = port;
    }

    public String getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
