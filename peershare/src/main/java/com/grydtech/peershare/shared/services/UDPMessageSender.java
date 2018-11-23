package com.grydtech.peershare.shared.services;

import com.grydtech.peershare.client.models.Node;
import com.grydtech.peershare.shared.models.SerializableMessage;

import java.io.IOException;

public interface UDPMessageSender {

    void sendMessage(SerializableMessage message, Node destinationNode) throws IOException;
}
