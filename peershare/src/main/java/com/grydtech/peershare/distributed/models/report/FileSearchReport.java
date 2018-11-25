package com.grydtech.peershare.distributed.models.report;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

public class FileSearchReport {

    private String searchId;
    private int maxHopsReached = 0;

    @JsonIgnore
    private Date startTime = new Date();
    @JsonIgnore
    private Date firstResponseTime;
    @JsonIgnore
    private Date firstSuccessResponseTime;
    @JsonIgnore
    private Set<String> nodesAnswered = new HashSet<>();

    public FileSearchReport(String searchId) {
        this.searchId = searchId;
    }

    public String getSearchId() {
        return searchId;
    }

    public int getNodesAnswered() {
        return nodesAnswered.size();
    }

    public int getMaxHopsReached() {
        return maxHopsReached;
    }

    public long getResponseTime() {
        return (firstResponseTime != null) ? firstResponseTime.getTime() - startTime.getTime() : -1;
    }

    public long getSuccessResponseTime() {
        return (firstSuccessResponseTime != null) ? firstSuccessResponseTime.getTime() - startTime.getTime() : -1;
    }

    public void submitResponse(int fileCount, int hops, String nodeId) {
        if (firstResponseTime == null) {
            firstResponseTime = new Date();
        }

        if (firstSuccessResponseTime == null && fileCount > 0) {
            firstSuccessResponseTime = new Date();
        }

        nodesAnswered.add(nodeId);
        maxHopsReached = (maxHopsReached < hops) ? hops : maxHopsReached;
    }
}
