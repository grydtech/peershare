package com.grydtech.peershare.files.models;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class FileInfo {

    private String id;
    private String name;

    public FileInfo(String name) {
        this.id = new String(Base64.getEncoder().encode(name.getBytes()), StandardCharsets.UTF_8);
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
