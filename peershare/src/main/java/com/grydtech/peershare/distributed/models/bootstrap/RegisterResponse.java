package com.grydtech.peershare.distributed.models.bootstrap;

import com.grydtech.peershare.distributed.models.Command;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.shared.models.DeserializableMessage;

import java.util.ArrayList;
import java.util.List;

public class RegisterResponse implements DeserializableMessage {

    private BootstrapResponseStatus status;
    private List<Node> nodes;

    public BootstrapResponseStatus getStatus() {
        return status;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    @Override
    public void deserialize(String message) {
        String[] parts = message.split(" ");
        if (!Command.REGISTER_OK.toString().equals(parts[1])) return;

        this.status = BootstrapResponseStatus.byCode(Integer.parseInt(parts[2]));

        this.nodes = new ArrayList<>();

        if (this.status == BootstrapResponseStatus.SUCCESSFUL) {
            for (int i = 3; i < parts.length; i = i + 3) {
                Node node = new Node(parts[i], Integer.parseInt(parts[i + 1]));
                this.nodes.add(node);
            }
        }
    }
}
