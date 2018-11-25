package com.grydtech.peershare.web.controllers;

import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.report.models.FileSearchSummaryReport;
import com.grydtech.peershare.distributed.services.ClusterManager;
import com.grydtech.peershare.files.models.FileInfo;
import com.grydtech.peershare.files.services.FileStore;
import com.grydtech.peershare.report.models.FileSearchReport;
import com.grydtech.peershare.report.models.NodeReport;
import com.grydtech.peershare.report.services.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class InformationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InformationController.class);

    private final ClusterManager clusterManager;
    private final FileStore fileStore;
    private final Reporter reporter;

    @Autowired
    public InformationController(ClusterManager clusterManager, FileStore fileStore, Reporter reporter) {
        this.clusterManager = clusterManager;
        this.fileStore = fileStore;
        this.reporter = reporter;
    }

    @GetMapping("routing-table")
    public ResponseEntity<Collection<Node>> getRoutingTable() {
        LOGGER.info("get routing table request received");

        Collection<Node> knownNodes = clusterManager.getConnectedCluster();

        LOGGER.info("send routing table count: \"{}\"", knownNodes.size());

        return ResponseEntity.ok(knownNodes);
    }

    @GetMapping("file-list")
    public ResponseEntity<Collection<FileInfo>> getFileList() {
        LOGGER.info("get routing table request received");

        Collection<FileInfo> files = fileStore.getAll();

        LOGGER.info("send file list count: \"{}\"", files.size());

        return ResponseEntity.ok(files);
    }

    @GetMapping("node-report")
    public ResponseEntity<NodeReport> getNodeReport() {
        LOGGER.info("get node report request received");

        return ResponseEntity.ok(reporter.getNodeReport());
    }

    @GetMapping("search-report")
    public ResponseEntity<Collection<FileSearchReport>> getSearchReport() {
        LOGGER.info("get search report request received");

        return ResponseEntity.ok(reporter.getSearchReports());
    }

    @GetMapping("search-summary")
    public ResponseEntity<FileSearchSummaryReport> getSearchSummary() {
        LOGGER.info("get search summary request received");

        return ResponseEntity.ok(reporter.getSearchSummaryReport());
    }
}
