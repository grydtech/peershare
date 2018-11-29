package com.grydtech.peershare.files.services.impl;

import com.grydtech.peershare.files.models.FileInfo;
import com.grydtech.peershare.files.services.FileStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Service
public class FileStoreImpl implements FileStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStoreImpl.class);
    private static final Random random = new Random();

    private final Map<String, FileInfo> fileIndex = new HashMap<>();

    @Value("${file-store.input-file}")
    private String inputFileLocation;

    @Value("${file-store.min-size}")
    private int min;

    @Value("${file-store.max-size}")
    private int max;

    @Override
    public void add(String id, FileInfo fileInfo) {
        if (!fileIndex.containsKey(id)) {
            fileIndex.put(id, fileInfo);

            LOGGER.info("FILES: file: \"{}\" added to file index with fileId: \"{}\"", fileInfo.getName(), id);
        }
    }

    @Override
    public void remove(String id) {
        if (fileIndex.containsKey(id)) {
            fileIndex.remove(id);

            LOGGER.info("FILES: fileId: \"{}\" removed from file index", id);
        }
    }

    @Override
    public List<FileInfo> search(String keyword) {
        List<FileInfo> files = new ArrayList<>();
        String[] queryTokens = keyword.trim().split(" ");

        LOGGER.trace("FILES: query tokens: \"{}\"", Arrays.toString(queryTokens));

        for (FileInfo f: fileIndex.values()) {
            String[] fileNameTokens = f.getName().split(" ");

            LOGGER.trace("FILES: file name tokens: \"{}\"", Arrays.toString(fileNameTokens));

            search:
            for (String s1: queryTokens) {
                for (String s2: fileNameTokens) {
                    if (s1.toLowerCase().equals(s2.toLowerCase())) {
                        files.add(f);

                        LOGGER.trace("FILES: match found");

                        break search;
                    }
                }
            }
        }

        LOGGER.trace("FILES: \"{}\" matches found for search query: \"{}\"", files.size(), keyword);

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
    public void index() {
        LOGGER.info("FILES: file indexing started");

        fileIndex.clear();

        List<String> fileNames = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(new File(inputFileLocation)))) {
            String fileName = br.readLine();

            while (fileName != null) {
                fileNames.add(fileName.trim());
                fileName = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int bound = fileNames.size();

        LOGGER.info("FILES: file list read, \"{}\" file names available", bound);

        if (fileNames.size() <= min) {
            fileNames.stream().map(FileInfo::new).forEach(f -> add(f.getId(), f));
            return;
        }

        while (fileIndex.size() < min) {
            int index = random.nextInt(bound);
            FileInfo fileInfo = new FileInfo(fileNames.get(index));
            this.add(fileInfo.getId(), fileInfo);
        }

        for (int i = 0; i < max - min; i++) {
            int index = random.nextInt(bound);
            FileInfo fileInfo = new FileInfo(fileNames.get(index));
            this.add(fileInfo.getId(), fileInfo);
        }

        LOGGER.info("FILES: \"{}\" file names added to file index", fileIndex.size());

        LOGGER.info("FILES: file indexing completed");
    }
}
