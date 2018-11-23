package com.grydtech.peershare.client.models.search;

import com.grydtech.peershare.client.models.Node;
import com.grydtech.peershare.files.models.FileInfo;

import java.util.List;

public class FileSearchResult {

    private Node node;
    private List<FileInfo> files;
    private int hops;

    public FileSearchResult(Node node, List<FileInfo> files, int hops) {
        this.node = node;
        this.files = files;
        this.hops = hops;
    }

    public Node getNode() {
        return node;
    }

    public List<FileInfo> getFiles() {
        return files;
    }

    public int getHops() {
        return hops;
    }
}
