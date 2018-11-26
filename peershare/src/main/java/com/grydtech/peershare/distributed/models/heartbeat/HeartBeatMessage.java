package com.grydtech.peershare.distributed.models.heartbeat;

import com.grydtech.peershare.distributed.models.Command;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.shared.models.DeserializableMessage;
import com.grydtech.peershare.shared.models.Message;
import com.grydtech.peershare.shared.models.SerializableMessage;

import java.util.UUID;

public class HeartBeatMessage extends Message implements SerializableMessage, DeserializableMessage {

    private Node node;
    private int hop;

    public HeartBeatMessage() {
    }

    public HeartBeatMessage(Node node, int hop) {
        this.node = node;
        this.hop = hop;

        this.messageId = UUID.randomUUID();
    }

    public Node getNode() {
        return node;
    }

    public int getHop() {
        return hop;
    }

    public boolean isMaxHopsReached(int maxHops) {
        return hop > maxHops;
    }

    @Override
    public void deserialize(String message) {
        String[] parts = message.split(" ");
        if (!Command.HEART_BEAT.toString().equals(parts[2])) return;

        this.messageId = UUID.fromString(parts[1]);
        this.hop = Integer.parseInt(parts[3]);
        this.node = new Node(parts[4], Integer.parseInt(parts[5]));
    }

    @Override
    public String serialize() {
        String s = String.format("%s %s %d %s %d", this.messageId.toString(), Command.HEART_BEAT.toString(), hop, node.getHost(), node.getPort());
        return String.format("%04d %s", s.length() + 5, s);
    }
}
