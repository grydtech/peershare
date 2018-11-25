package com.grydtech.peershare.distributed.models.search;

public enum FileSearchResponseStatus {
    SUCCESSFUL,
    NO_RESULT,
    COMMAND_ERROR,
    UNKNOWN;

    public static FileSearchResponseStatus fromCode(int code) {
        if (code == 0) {
            return NO_RESULT;
        } else if (code == 9999 || code == 9998) {
            return COMMAND_ERROR;
        } else if (code > 0 && code < 9996) {
            return SUCCESSFUL;
        } else {
            return UNKNOWN;
        }
    }
}
