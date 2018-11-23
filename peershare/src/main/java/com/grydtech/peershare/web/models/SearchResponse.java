package com.grydtech.peershare.web.models;

public class SearchResponse {

    private String searchId;
    private SearchResult searchResult;

    public SearchResponse(String searchId, SearchResult searchResult) {
        this.searchId = searchId;
        this.searchResult = searchResult;
    }

    public String getSearchId() {
        return searchId;
    }

    public SearchResult getSearchResult() {
        return searchResult;
    }
}
