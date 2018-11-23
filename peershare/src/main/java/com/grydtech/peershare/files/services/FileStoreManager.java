package com.grydtech.peershare.files.services;

import com.grydtech.peershare.files.models.FileInfo;
import com.grydtech.peershare.shared.services.Manager;

import java.util.List;

public interface FileStoreManager extends Manager {
    void add(String id, FileInfo fileInfo);

    void remove(String id);

    List<FileInfo> search(String keyword);

    boolean has(String id);

    List<FileInfo> getAll();

    FileInfo get(String id);
}
