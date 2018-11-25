package com.grydtech.peershare.distributed.models.peer;

import com.grydtech.peershare.distributed.models.Command;
import com.grydtech.peershare.shared.models.DeserializableMessage;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.shared.models.Message;
import com.grydtech.peershare.shared.models.SerializableMessage;

import java.util.UUID;

public class PeerJoinRequest extends Message implements SerializableMessage, DeserializableMessage {

    private Node node;

    public PeerJoinRequest() {
    }

    public PeerJoinRequest(Node node) {
        this.node = node;

        this.messageId = UUID.randomUUID();
    }

    public Node getNode() {
        return node;
    }

    @Override
    public void deserialize(String message) {
        String[] parts = message.split(" ");
        if (!Command.JOIN.toString().equals(parts[2])) return;

        this.messageId = UUID.fromString(parts[1]);
        this.node = new Node(parts[3], Integer.parseInt(parts[4]));
    }

    @Override
    public String serialize() {
        String s = String.format("%s %s %s %d", this.messageId.toString(), Command.JOIN.toString(), this.node.getHost(), this.node.getPort());
        return String.format("%04d %s", s.length() + 5, s);
    }
}
