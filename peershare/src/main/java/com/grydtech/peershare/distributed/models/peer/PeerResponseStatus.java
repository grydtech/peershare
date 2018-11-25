package com.grydtech.peershare.distributed.models.peer;

public enum PeerResponseStatus {
    SUCCESSFUL(0),
    COMMAND_ERROR(9999),
    UNKNOWN(-1);

    private int code;

    PeerResponseStatus(int code) {
        this.code = code;
    }

    public static PeerResponseStatus byCode(int code) {
        if (code == 0) {
            return SUCCESSFUL;
        } else if (code == 9999) {
            return COMMAND_ERROR;
        } else {
            return UNKNOWN;
        }
    }

    public int getCode() {
        return this.code;
    }
}
