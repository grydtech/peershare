package com.grydtech.peershare.web.controllers;

import com.grydtech.peershare.files.models.FileInfo;
import com.grydtech.peershare.files.services.FileStore;
import com.grydtech.peershare.files.services.TempFileCreator;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Controller
@CrossOrigin
public class FileDownloadController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadController.class);

    private final FileStore fileStore;
    private final TempFileCreator tempFileCreator;

    @Autowired
    public FileDownloadController(FileStore fileStore, TempFileCreator tempFileCreator) {
        this.fileStore = fileStore;
        this.tempFileCreator = tempFileCreator;
    }

    @GetMapping("download/{id}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String id) throws IOException {
        LOGGER.info("file download request received for fileId: \"{}\"", id);

        if (!fileStore.has(id)) return ResponseEntity.notFound().build();

        FileInfo fileInfo = fileStore.get(id);
        File file = tempFileCreator.createTempFile(fileInfo.getName());

        String sha1Hash = DigestUtils.sha1Hex(new FileInputStream(file)).toUpperCase();

        LOGGER.info("generated hash value: \"{}\"", sha1Hash);

        InputStreamResource inputStreamResource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header("Checksum-SHA1", sha1Hash)
                .contentLength(file.length())
                .body(inputStreamResource);
    }
}
