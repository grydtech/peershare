package com.grydtech.peershare.files.services.impl;

import com.grydtech.peershare.files.services.TempFileCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

@Service
public class TempFileCreatorImpl implements TempFileCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempFileCreatorImpl.class);
    private static final String tmpdir = System.getProperty("java.io.tmpdir");
    private static final Random random = new Random();

    @Override
    public File createTempFile(String fileName) throws IOException {
        Path filePath = Paths.get(tmpdir + File.separator + fileName);

        LOGGER.info("generate temporary file in: \"{}\"", filePath.toAbsolutePath().toString());

        int length = (random.nextInt(10) + 1) * 1024 * 1024;
        byte[] data = new byte[length];
        random.nextBytes(data);

        Files.write(filePath, data);

        LOGGER.info("temporary file generated with random content");

        return new File(filePath.toAbsolutePath().toString());
    }
}
