package com.grydtech.peershare.web.models;

public class SearchFailedResponse {
    private int errorCode;
    private String errorMessage;

    public SearchFailedResponse(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
