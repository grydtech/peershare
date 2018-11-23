package com.grydtech.peershare.filevalidator;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileValidator.class);

    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);

        String md5Hash = DigestUtils.md5Hex(new FileInputStream(file));

        LOGGER.info("generated checksum (md5) for file: \"{}\"", md5Hash);
        LOGGER.info("validate generated value: \"{}\" against passed value: \"{}\"", md5Hash, args[1]);

        if (md5Hash.equals(args[1])) {
            LOGGER.info("validation successful");
        } else {
            LOGGER.info("validation failed");
        }
    }
}
