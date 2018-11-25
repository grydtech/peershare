package com.grydtech.peershare.shared.services;

import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.shared.models.SerializableMessage;

import java.io.IOException;

public interface TCPMessageSender {

    String sendMessage(SerializableMessage message, Node destinationNode) throws IOException;
}
