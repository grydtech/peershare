package com.grydtech.peershare.client.models.search;

import com.grydtech.peershare.client.models.Command;
import com.grydtech.peershare.shared.models.DeserializableMessage;
import com.grydtech.peershare.client.models.Node;
import com.grydtech.peershare.shared.models.Message;
import com.grydtech.peershare.shared.models.SerializableMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class FileSearchResponse extends Message implements SerializableMessage, DeserializableMessage {

    private Node node;
    private List<String> fileNames;
    private int hops;
    private FileSearchResponseStatus status;

    public FileSearchResponse() {
    }

    public FileSearchResponse(Node node, List<String> fileNames, UUID messageId, int hops, FileSearchResponseStatus status) {
        this.node = node;
        this.fileNames = fileNames;
        this.hops = hops;
        this.messageId = messageId;
        this.status = status;
    }

    public Node getNode() {
        return node;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public int getHops() {
        return hops;
    }

    public FileSearchResponseStatus getStatus() {
        return status;
    }

    @Override
    public void deserialize(String message) {
        String[] parts = message.split(" ");
        if (!Command.SEARCH_OK.toString().equals(parts[2])) return;

        this.messageId = UUID.fromString(parts[1]);
        this.status = FileSearchResponseStatus.fromCode(Integer.parseInt(parts[3]));
        this.node = new Node(parts[4], Integer.parseInt(parts[5]));
        this.hops = Integer.parseInt(parts[6]);

        this.fileNames = new ArrayList<>();

        if (this.status == FileSearchResponseStatus.SUCCESSFUL) {
            fileNames.addAll(Arrays.asList(parts).subList(7, parts.length));
        }
    }

    @Override
    public String serialize() {
        StringBuilder sb = new StringBuilder(String.format("%s %s %d %s %d %d", this.messageId.toString(), Command.SEARCH_OK.toString(), fileNames.size(), this.node.getHost(), this.node.getPort(), this.hops));
        fileNames.forEach(fn -> sb.append(" ").append(fn));

        String s = sb.toString();
        return String.format("%04d %s", s.length() + 5, s);
    }
}
