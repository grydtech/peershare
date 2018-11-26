package com.grydtech.peershare.shared.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JSONHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONHelper.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private JSONHelper() {
    }

    private static String toJson(Object object) {
        if (object == null) return "null object passed";

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return "error occurred when serializing object to json";
    }
}
