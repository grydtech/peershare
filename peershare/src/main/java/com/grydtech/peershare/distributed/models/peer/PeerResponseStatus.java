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
        switch (code) {
            case 0:
                return SUCCESSFUL;
            case 9999:
                return COMMAND_ERROR;
            default:
                return UNKNOWN;
        }
    }

    public int getCode() {
        return this.code;
    }
}
