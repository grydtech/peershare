package com.grydtech.peershare.report.services.impl;

import com.grydtech.peershare.report.models.FileSearchReport;
import com.grydtech.peershare.report.models.FileSearchSummaryReport;
import com.grydtech.peershare.report.models.NodeReport;
import com.grydtech.peershare.report.services.Reporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReporterImpl implements Reporter {

    private final NodeReport nodeReport = new NodeReport();
    private final Map<String, FileSearchReport> searchReportMap = new HashMap<>();

    @Value("${search.max-hops}")
    private int searchMaxHops;

    @Override
    public void reportSearchStarted(UUID searchId) {
        nodeReport.searchStarted();

        if (searchMaxHops >= 1) {
            nodeReport.searchForwarded();
        }

        FileSearchReport fileSearchReport = new FileSearchReport(searchId.toString());
        searchReportMap.put(searchId.toString(), fileSearchReport);
    }

    @Override
    public synchronized void reportSearchAccepted(UUID searchId, int hop) {
        nodeReport.searchAccepted();

        if (searchMaxHops >= hop + 1) {
            nodeReport.searchForwarded();
        }
    }

    @Override
    public synchronized void reportResultReceived(UUID searchId, int fileCount, int hops, String nodeId) {
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
    public Collection<FileSearchReport> getSearchReports() {
        return searchReportMap.values();
    }

    @Override
    public synchronized FileSearchSummaryReport getSearchSummaryReport() {
        FileSearchSummaryReport fileSearchSummaryReport = new FileSearchSummaryReport();
        searchReportMap.values().forEach(r -> fileSearchSummaryReport.submitSuccessResponseTime(r.getSuccessResponseTime()));
        return fileSearchSummaryReport;
    }
}
