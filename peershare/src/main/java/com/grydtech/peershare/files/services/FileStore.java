package com.grydtech.peershare.files.services;

import com.grydtech.peershare.files.models.FileInfo;

import java.util.List;

public interface FileStore {
    void add(String id, FileInfo fileInfo);

    void remove(String id);

    List<FileInfo> search(String keyword);

    boolean has(String id);

    List<FileInfo> getAll();

    FileInfo get(String id);

    void index();
}
