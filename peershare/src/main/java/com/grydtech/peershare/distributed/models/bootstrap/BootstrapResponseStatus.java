package com.grydtech.peershare.distributed.models.bootstrap;

public enum BootstrapResponseStatus {
    SUCCESSFUL,
    ERROR,
    ALREADY_REGISTERED,
    BOOTSTRAP_SERVER_FULL,
    UNKNOWN;

    public static BootstrapResponseStatus byCode(int code) {
        if (code == 9999) {
            return ERROR;
        } else if (code == 9998) {
            return ALREADY_REGISTERED;
        } else if (code == 9997) {
            return BOOTSTRAP_SERVER_FULL;
        } else if (code >= 0 && code < 9996) {
            return SUCCESSFUL;
        } else {
            return UNKNOWN;
        }
    }
}
