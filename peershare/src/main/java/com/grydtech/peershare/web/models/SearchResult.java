package com.grydtech.peershare.web.models;

public class SearchResult {

    private String fileId;
    private String fileName;
    private String host;
    private int port;
    private int hops;

    public SearchResult(String fileId, String fileName, String host, int port, int hops) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.host = host;
        this.port = port;
        this.hops = hops;
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

    public int getHops() {
        return hops;
    }
}
