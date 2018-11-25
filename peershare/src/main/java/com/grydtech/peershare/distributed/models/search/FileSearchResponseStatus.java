package com.grydtech.peershare.distributed.models.search;

public enum FileSearchResponseStatus {
    SUCCESSFUL,
    NO_RESULT,
    COMMAND_ERROR;

    public static FileSearchResponseStatus fromCode(int code) {
        if (code == 0) {
            return FileSearchResponseStatus.NO_RESULT;
        } else if (code == 9999 || code == 9998) {
            return FileSearchResponseStatus.COMMAND_ERROR;
        } else {
            return FileSearchResponseStatus.SUCCESSFUL;
        }
    }
}
