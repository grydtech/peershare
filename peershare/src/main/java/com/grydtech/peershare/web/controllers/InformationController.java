package com.grydtech.peershare.web.controllers;

import com.grydtech.peershare.client.models.Node;
import com.grydtech.peershare.client.services.ClusterManager;
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

    @Autowired
    public InformationController(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    @GetMapping("routing-table")
    public ResponseEntity<List<Node>> getRoutingTable() {
        LOGGER.info("get routing table request received");

        List<Node> knownNodes = clusterManager.getConnectedCluster();

        LOGGER.info("send routing table count: \"{}\"", knownNodes.size());

        return ResponseEntity.ok(knownNodes);
    }
}
