package com.grydtech.peershare.distributed.models.bootstrap;

import com.grydtech.peershare.distributed.models.Command;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.shared.models.SerializableMessage;

public class UnregisterRequest implements SerializableMessage {

    private Node node;

    public UnregisterRequest(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    @Override
    public String serialize() {
        String s = String.format("%s %s %d %s", Command.UNREGISTER.toString(), this.node.getHost(), this.node.getPort(), this.getNode().getUsername());
        return String.format("%04d %s", s.length() + 5, s);
    }
}
