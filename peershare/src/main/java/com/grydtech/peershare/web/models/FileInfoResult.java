package com.grydtech.peershare.web.models;

public class FileInfoResult {
    private String hash;

    public FileInfoResult(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }
}
