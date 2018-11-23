package com.grydtech.peershare.client.models.bootstrap;

public enum BootstrapResponseStatus {
    SUCCESSFUL,
    ERROR,
    ALREADY_REGISTERED,
    BOOTSTRAP_SERVER_FULL,
    UNKNOWN;

    public static BootstrapResponseStatus byCode(int code) {
        switch (code) {
            case 0:
                return SUCCESSFUL;
            case 1:
                return SUCCESSFUL;
            case 2:
                return SUCCESSFUL;
            case 9999:
                return ERROR;
            case 9998:
                return ALREADY_REGISTERED;
            case 9997:
                return BOOTSTRAP_SERVER_FULL;
            default:
                return UNKNOWN;
        }
    }
}
