package com.grydtech.peershare.distributed.models.gossip;

import com.grydtech.peershare.distributed.models.Command;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.shared.models.DeserializableMessage;
import com.grydtech.peershare.shared.models.Message;
import com.grydtech.peershare.shared.models.SerializableMessage;

import java.util.UUID;

public class NodeDiscoveredGossip extends Message implements SerializableMessage, DeserializableMessage {

    private Node discoveredNode;
    private int hop;

    public NodeDiscoveredGossip() {
    }

    public NodeDiscoveredGossip(Node discoveredNode, int hop) {
        this.discoveredNode = discoveredNode;
        this.hop = hop;

        this.messageId = UUID.randomUUID();
    }

    public Node getDiscoveredNode() {
        return discoveredNode;
    }

    public int getHop() {
        return hop;
    }

    public boolean isMaxHopsReached(int maxHops) {
        return hop >= maxHops;
    }

    @Override
    public void deserialize(String message) {
        String[] parts = message.split(" ");
        if (!Command.NODE_DISCOVERED.toString().equals(parts[2])) return;

        this.messageId = UUID.fromString(parts[1]);
        this.hop = Integer.parseInt(parts[3]);
        this.discoveredNode = new Node(parts[4], Integer.parseInt(parts[5]));
    }

    @Override
    public String serialize() {
        String s = String.format("%s %s %d %s %d", this.messageId.toString(), Command.NODE_DISCOVERED.toString(), this.hop, this.discoveredNode.getHost(), this.discoveredNode.getPort());
        return String.format("%04d %s", s.length() + 5, s);
    }
}
