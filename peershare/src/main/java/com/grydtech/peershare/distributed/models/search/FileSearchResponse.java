package com.grydtech.peershare.distributed.models.search;

import com.grydtech.peershare.distributed.models.Command;
import com.grydtech.peershare.shared.models.DeserializableMessage;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.shared.models.Message;
import com.grydtech.peershare.shared.models.SerializableMessage;

import java.nio.charset.StandardCharsets;
import java.util.*;

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
            for (String encoded: Arrays.asList(parts).subList(7, parts.length)) {
                fileNames.add(new String(Base64.getDecoder().decode(encoded.getBytes()), StandardCharsets.UTF_8));
            }
        }
    }

    @Override
    public String serialize() {
        StringBuilder sb = new StringBuilder(String.format("%s %s %d %s %d %d", this.messageId.toString(), Command.SEARCH_OK.toString(), fileNames.size(), this.node.getHost(), this.node.getPort(), this.hops));
        fileNames.forEach(fn -> sb.append(" ").append(new String(Base64.getEncoder().encode(fn.getBytes()), StandardCharsets.UTF_8)));

        String s = sb.toString();
        return String.format("%04d %s", s.length() + 5, s);
    }
}
