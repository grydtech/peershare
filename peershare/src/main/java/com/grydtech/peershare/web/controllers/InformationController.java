package com.grydtech.peershare.web.controllers;

import com.grydtech.peershare.client.models.Node;
import com.grydtech.peershare.client.services.ClusterManager;
import com.grydtech.peershare.files.models.FileInfo;
import com.grydtech.peershare.files.services.FileStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class InformationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InformationController.class);

    private final ClusterManager clusterManager;
    private final FileStore fileStore;

    @Autowired
    public InformationController(ClusterManager clusterManager, FileStore fileStore) {
        this.clusterManager = clusterManager;
        this.fileStore = fileStore;
    }

    @GetMapping("routing-table")
    public ResponseEntity<List<Node>> getRoutingTable() {
        LOGGER.info("get routing table request received");

        List<Node> knownNodes = clusterManager.getConnectedCluster();

        LOGGER.info("send routing table count: \"{}\"", knownNodes.size());

        return ResponseEntity.ok(knownNodes);
    }

    @GetMapping("file-list")
    public ResponseEntity<List<FileInfo>> getFileList() {
        LOGGER.info("get routing table request received");

        List<FileInfo> files = fileStore.getAll();

        LOGGER.info("send file list count: \"{}\"", files.size());

        return ResponseEntity.ok(files);
    }
}
