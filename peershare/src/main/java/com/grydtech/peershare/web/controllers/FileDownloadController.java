package com.grydtech.peershare.web.controllers;

import com.grydtech.peershare.files.models.FileInfo;
import com.grydtech.peershare.files.services.FileStoreManager;
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

    private final FileStoreManager fileStoreManager;
    private final TempFileCreator tempFileCreator;

    @Autowired
    public FileDownloadController(FileStoreManager fileStoreManager, TempFileCreator tempFileCreator) {
        this.fileStoreManager = fileStoreManager;
        this.tempFileCreator = tempFileCreator;
    }

    @GetMapping("download/{id}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String id) throws IOException {
        LOGGER.info("file download request received for fileId: \"{}\"", id);

        if (!fileStoreManager.has(id)) return ResponseEntity.notFound().build();

        FileInfo fileInfo = fileStoreManager.get(id);
        File file = tempFileCreator.createTempFile(fileInfo.getName());

        String md5Hash = DigestUtils.md5Hex(new FileInputStream(file));
        LOGGER.info("startService sending file, checksum value (md5): \"{}\"", md5Hash);

        InputStreamResource inputStreamResource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .contentLength(file.length())
                .body(inputStreamResource);
    }
}
