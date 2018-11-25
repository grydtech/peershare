package com.grydtech.peershare.distributed.services;

import com.grydtech.peershare.distributed.models.report.FileSearchReport;
import com.grydtech.peershare.distributed.models.report.FileSearchSummaryReport;
import com.grydtech.peershare.distributed.models.report.NodeReport;

import java.util.Collection;
import java.util.UUID;

public interface FileSearchReporter {

    void searchStarted(UUID searchId);

    void searchAccepted();

    void searchForwarded();

    void resultReceived(UUID searchId, int fileCount, int hops, String nodeId);

    NodeReport getNodeReport();

    Collection<FileSearchReport> getFileSearchReports();

    FileSearchSummaryReport getFileSearchSummary();
}
