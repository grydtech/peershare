package com.grydtech.peershare.shared.models;

public interface DeserializableMessage {
    void deserialize(String message) throws Exception;
}
