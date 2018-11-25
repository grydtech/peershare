package com.grydtech.peershare.distributed.models.peer;

public enum PeerJoinResponseStatus {
    SUCCESSFUL(0),
    COMMAND_ERROR(9999),
    UNKNOWN(-1);

    private int code;

    PeerJoinResponseStatus(int code) {
        this.code = code;
    }

    public static PeerJoinResponseStatus byCode(int code) {
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
