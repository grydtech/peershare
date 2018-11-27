package com.grydtech.peershare.distributed.models.gossip;

import com.grydtech.peershare.distributed.models.Command;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.shared.models.DeserializableMessage;
import com.grydtech.peershare.shared.models.Message;
import com.grydtech.peershare.shared.models.SerializableMessage;

import java.util.UUID;

public class GossipMessage extends Message implements SerializableMessage, DeserializableMessage {

    private Node discoveredNode;

    public GossipMessage() {
    }

    public GossipMessage(Node discoveredNode) {
        this.discoveredNode = discoveredNode;

        this.messageId = UUID.randomUUID();
    }

    public Node getDiscoveredNode() {
        return discoveredNode;
    }

    @Override
    public void deserialize(String message) {
        String[] parts = message.split(" ");
        if (!Command.GOSSIP.toString().equals(parts[2])) return;

        this.messageId = UUID.fromString(parts[1]);
        this.discoveredNode = new Node(parts[3], Integer.parseInt(parts[4]));
    }

    @Override
    public String serialize() {
        String s = String.format("%s %s %s %d", this.messageId.toString(), Command.GOSSIP.toString(), this.discoveredNode.getHost(), this.discoveredNode.getPort());
        return String.format("%04d %s", s.length() + 5, s);
    }
}
