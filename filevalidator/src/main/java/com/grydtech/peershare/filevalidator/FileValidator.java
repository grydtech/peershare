package com.grydtech.peershare.filevalidator;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;

public class FileValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileValidator.class);

    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        String mode = args[2];

        String generatedHash;

        if ("md5".equals(mode.toLowerCase())) {
            generatedHash = DigestUtils.md5Hex(new FileInputStream(file)).toUpperCase();
        } else if ("sha1".equals(mode.toLowerCase())) {
            generatedHash = DigestUtils.sha1Hex(new FileInputStream(file)).toUpperCase();
        } else {
            LOGGER.error("unknown command, please send a command similar to this \"java -jar filevalidator-1.0-SNAPSHOT.jar [hash-value] [md5 | sha1]\"");
            throw new RuntimeException("unknown command");
        }

        LOGGER.info("generated checksum ({}) for file: \"{}\"", mode, generatedHash);

        LOGGER.info("validate ({}) generated value: \"{}\" against passed value: \"{}\"", mode, generatedHash, args[1]);

        if (generatedHash.equals(args[1])) {
            LOGGER.info("validation successful");
        } else {
            LOGGER.info("validation failed");
        }
    }
}
