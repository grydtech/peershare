package com.grydtech.peershare.distributed.models.bootstrap;

import com.grydtech.peershare.distributed.models.Command;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.shared.models.SerializableMessage;

public class RegisterRequest implements SerializableMessage {

    private final Node node;
    private String username;

    public RegisterRequest(Node node, String username) {
        this.node = node;
        this.username = username;
    }

    public Node getNode() {
        return node;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String serialize() {
        String s = String.format("%s %s %d %s", Command.REGISTER.toString(), this.node.getHost(), this.node.getPort(), this.username);
        return String.format("%04d %s", s.length() + 5, s);
    }
}
