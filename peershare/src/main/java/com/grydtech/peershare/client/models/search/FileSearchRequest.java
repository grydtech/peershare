package com.grydtech.peershare.client.models.search;

import com.grydtech.peershare.client.models.Command;
import com.grydtech.peershare.client.models.Node;
import com.grydtech.peershare.shared.models.DeserializableMessage;
import com.grydtech.peershare.shared.models.Message;
import com.grydtech.peershare.shared.models.SerializableMessage;

import java.util.UUID;

public class FileSearchRequest extends Message implements SerializableMessage, DeserializableMessage {

    private Node node;
    private String keyword;

    private int hop;

    public FileSearchRequest() {
    }

    public FileSearchRequest(Node node, String keyword, UUID requestId, int hop) {
        this.node = node;
        this.keyword = keyword;
        this.messageId = requestId;
        this.hop = hop;
    }

    public Node getNode() {
        return node;
    }

    public String getKeyword() {
        return keyword;
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
        if (!Command.SEARCH.toString().equals(parts[2])) return;

        this.messageId = UUID.fromString(parts[1]);
        this.node = new Node(parts[3], Integer.parseInt(parts[4]));
        this.keyword = parts[5];
        this.hop = Integer.parseInt(parts[6]);
    }

    @Override
    public String serialize() {
        String s = String.format("%s %s %s %d %s %d", this.messageId.toString(), Command.SEARCH.toString(), this.node.getHost(), this.node.getPort(), this.keyword, this.hop);
        return String.format("%04d %s", s.length() + 5, s);
    }
}
