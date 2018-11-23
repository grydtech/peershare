package com.grydtech.peershare.client.models.peer;

import com.grydtech.peershare.client.models.Command;
import com.grydtech.peershare.client.models.Node;
import com.grydtech.peershare.shared.models.DeserializableMessage;
import com.grydtech.peershare.shared.models.Message;
import com.grydtech.peershare.shared.models.SerializableMessage;

import java.util.UUID;

public class PeerLeaveRequest extends Message implements SerializableMessage, DeserializableMessage {

    private Node node;

    public PeerLeaveRequest() {
    }

    public PeerLeaveRequest(Node node) {
        this.node = node;

        this.messageId = UUID.randomUUID();
    }

    public Node getNode() {
        return node;
    }

    @Override
    public void deserialize(String message) {
        String[] parts = message.split(" ");
        if (!Command.LEAVE.toString().equals(parts[2])) return;

        this.messageId = UUID.fromString(parts[1]);
        this.node = new Node(parts[3], Integer.parseInt(parts[4]));
    }

    @Override
    public String serialize() {
        String s = String.format("%s %s %s %d", this.messageId.toString(), Command.LEAVE.toString(), this.node.getHost(), this.node.getPort());
        return String.format("%04d %s", s.length() + 5, s);
    }
}
