package com.grydtech.peershare.files.services.impl;

import com.grydtech.peershare.files.models.FileInfo;
import com.grydtech.peershare.files.services.FileStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class FileStoreManagerImpl implements FileStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStoreManagerImpl.class);

    private static final Map<String, FileInfo> fileIndex = new HashMap<>();

    @Value("${file-store.input-file}")
    private String inputFileLocation;

    @Override
    public void add(String id, FileInfo fileInfo) {
        fileIndex.put(id, fileInfo);
        LOGGER.info("file: \"{}\" added to the startService with fileId: \"{}\"", fileInfo.getName(), id);
    }

    @Override
    public void remove(String id) {
        fileIndex.remove(id);
        LOGGER.info("fileId: \"{}\" removed from the startService", id);
    }

    @Override
    public List<FileInfo> search(String keyword) {
        List<FileInfo> files = new ArrayList<>();
        fileIndex.values().stream()
                .filter(f -> f.getName().toLowerCase().contains(keyword.toLowerCase()))
                .forEach(files::add);

        return files;
    }

    @Override
    public boolean has(String id) {
        return fileIndex.containsKey(id);
    }

    @Override
    public List<FileInfo> getAll() {
        return new ArrayList<>(fileIndex.values());
    }

    @Override
    public FileInfo get(String id) {
        return fileIndex.get(id);
    }

    @Override
    public void startService() {
        LOGGER.info("file store manager started");
        LOGGER.info("file indexing started");

        try (BufferedReader br = new BufferedReader(new FileReader(new File(inputFileLocation)))) {
            Random random = new Random();
            String fileName = br.readLine();

            while (fileName != null) {
                if (random.nextBoolean()) {
                    FileInfo fileInfo = new FileInfo(fileName);
                    add(fileInfo.getId(), fileInfo);
                }

                fileName = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        LOGGER.info("file indexing completed");
    }

    @Override
    public void stopService() {
        fileIndex.clear();
        LOGGER.info("file indexing stopped");
    }
}
