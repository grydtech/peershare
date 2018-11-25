package com.grydtech.peershare.report.models;

public class NodeReport {
    private int searchesStarted = 0;
    private int searchedAccepted = 0;
    private int searchesForwarded = 0;
    private int responsesReceived = 0;

    public int getSearchesStarted() {
        return searchesStarted;
    }

    public int getSearchedAccepted() {
        return searchedAccepted;
    }

    public int getSearchesForwarded() {
        return searchesForwarded;
    }

    public int getResponsesReceived() {
        return responsesReceived;
    }

    public void searchStarted() {
        searchesStarted++;
    }

    public void searchAccepted() {
        searchedAccepted++;
    }

    public void searchForwarded() {
        searchesForwarded++;
    }

    public void responseReceived() {
        responsesReceived++;
    }
}
