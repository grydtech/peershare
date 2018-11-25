package com.grydtech.peershare.distributed.services.impl;

import com.grydtech.peershare.distributed.models.report.FileSearchReport;
import com.grydtech.peershare.distributed.models.report.NodeReport;
import com.grydtech.peershare.distributed.services.FileSearchReporter;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FileSearchReporterImpl implements FileSearchReporter {

    private final NodeReport nodeReport = new NodeReport();
    private final Map<String, FileSearchReport> searchReportMap = new HashMap<>();

    @Override
    public void searchStarted(UUID searchId) {
        nodeReport.searchStarted();
        FileSearchReport fileSearchReport = new FileSearchReport(searchId.toString());
        searchReportMap.put(searchId.toString(), fileSearchReport);
    }

    @Override
    public void searchAccepted() {
        nodeReport.searchAccepted();
    }

    @Override
    public void searchForwarded() {
        nodeReport.searchForwarded();
    }

    @Override
    public void resultReceived(UUID searchId, int fileCount, int hops, String nodeId) {
        nodeReport.responseReceived();

        FileSearchReport fileSearchReport = searchReportMap.get(searchId.toString());

        if (fileSearchReport != null) {
            fileSearchReport.submitResponse(fileCount, hops, nodeId);
        }
    }

    @Override
    public NodeReport getNodeReport() {
        return nodeReport;
    }

    @Override
    public Collection<FileSearchReport> getFileSearchReports() {
        return searchReportMap.values();
    }
}
