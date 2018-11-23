package com.grydtech.peershare.client.models.bootstrap;

public enum BootstrapResponseStatus {
    SUCCESSFUL,
    COMMAND_ERROR,
    ALREADY_REGISTERED_TO_YOU,
    ALREADY_REGISTERED_TO_OTHER,
    BOOTSTRAP_FULL,
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
                return COMMAND_ERROR;
            case 9998:
                return ALREADY_REGISTERED_TO_YOU;
            case 9997:
                return ALREADY_REGISTERED_TO_OTHER;
            case 9996:
                return BOOTSTRAP_FULL;
            default:
                return UNKNOWN;
        }
    }
}
