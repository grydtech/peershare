package com.grydtech.peershare.client.models.peer;

import com.grydtech.peershare.client.models.Command;
import com.grydtech.peershare.shared.models.DeserializableMessage;
import com.grydtech.peershare.client.models.Node;
import com.grydtech.peershare.shared.models.Message;
import com.grydtech.peershare.shared.models.SerializableMessage;

import java.util.UUID;

public class PeerJoinRequest extends Message implements SerializableMessage, DeserializableMessage {

    private Node newNode;

    public PeerJoinRequest() {
    }

    public PeerJoinRequest(Node newNode) {
        this.newNode = newNode;

        this.messageId = UUID.randomUUID();
    }

    public Node getNewNode() {
        return newNode;
    }

    @Override
    public void deserialize(String message) {
        String[] parts = message.split(" ");
        if (!Command.JOIN.toString().equals(parts[2])) return;

        this.messageId = UUID.fromString(parts[1]);
        this.newNode = new Node(parts[3], Integer.parseInt(parts[4]));
    }

    @Override
    public String serialize() {
        String s = String.format("%s %s %s %d", this.messageId.toString(), Command.JOIN.toString(), this.newNode.getHost(), this.newNode.getPort());
        return String.format("%04d %s", s.length() + 5, s);
    }
}
