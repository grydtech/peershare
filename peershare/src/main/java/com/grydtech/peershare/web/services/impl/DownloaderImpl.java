package com.grydtech.peershare.web.services.impl;

import com.grydtech.peershare.web.models.DownloadRequest;
import com.grydtech.peershare.web.models.DownloadResponse;
import com.grydtech.peershare.web.models.DownloadStatus;
import com.grydtech.peershare.web.services.Downloader;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;

@Service
public class DownloaderImpl implements Downloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloaderImpl.class);

    @Override
    public DownloadResponse download(DownloadRequest downloadRequest) throws IOException {
        LOGGER.info("start downloading file");

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(downloadRequest.getFileUrl(), HttpMethod.GET, entity, byte[].class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            return new DownloadResponse(downloadRequest.getFileName(), null, null, null, 0, DownloadStatus.DOWNLOAD_FAILED);
        }

        LOGGER.info("file downloaded successfully");
        LOGGER.info("write file content to disk");

        Files.write(Paths.get(downloadRequest.getFileName()), response.getBody());

        LOGGER.info("start content verification");

        String sha1HashReceived = Objects.requireNonNull(response.getHeaders().get("Checksum-SHA1")).get(0);

        LOGGER.info("received hash value: \"{}\"", sha1HashReceived);

        File file = new File(downloadRequest.getFileName());

        String sha1HashGenerated = DigestUtils.sha1Hex(new FileInputStream(file)).toUpperCase();

        LOGGER.info("generated hash value: \"{}\"", sha1HashGenerated);

        long fileSize = Long.parseLong(Objects.requireNonNull(response.getHeaders().get("Content-Length")).get(0));

        if (sha1HashGenerated.equals(sha1HashReceived)) {
            return new DownloadResponse(file.getName(), file.getAbsolutePath(), sha1HashReceived, sha1HashGenerated, fileSize, DownloadStatus.VALIDATION_SUCCESSFUL);
        } else {
            return new DownloadResponse(file.getName(), file.getAbsolutePath(), sha1HashReceived, sha1HashGenerated, fileSize, DownloadStatus.VALIDATION_FAILED);
        }
    }
}
