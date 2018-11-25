package com.grydtech.peershare.report.services;

import com.grydtech.peershare.report.models.FileSearchReport;
import com.grydtech.peershare.report.models.FileSearchSummaryReport;
import com.grydtech.peershare.report.models.NodeReport;

import java.util.Collection;
import java.util.UUID;

public interface Reporter {

    void reportSearchStarted(UUID searchId);

    void reportSearchAccepted(UUID searchId, int hop);

    void reportResultReceived(UUID searchId, int fileCount, int hops, String nodeId);

    NodeReport getNodeReport();

    Collection<FileSearchReport> getSearchReports();

    FileSearchSummaryReport getSearchSummaryReport();
}
