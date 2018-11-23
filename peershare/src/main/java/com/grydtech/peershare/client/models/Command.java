package com.grydtech.peershare.client.models;

public enum Command {
    REGISTER("REG"),
    REGISTER_OK("REGOK"),
    UNREGISTER("UNREG"),
    UNREGISTER_OK("UNROK"),
    JOIN("JOIN"),
    JOIN_OK("JOINOK"),
    LEAVE("LEAVE"),
    LEAVE_OK("LEAVEOK"),
    SEARCH("SER"),
    SEARCH_OK("SEROK"),
    NODE_DISCOVERED("GOSDIS"),
    NODE_UNRESPONSIVE("GOSUNR"),
    HEART_BEAT("HEART"),
    UNKNOWN("UNKNOWN");

    private String message;

    Command(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return this.message;
    }

    public static Command fromString(String string) {
        if ("REG".equals(string)) {
            return Command.REGISTER;
        } else if ("REGOK".equals(string)) {
            return Command.REGISTER_OK;
        } else if ("UNREG".equals(string)) {
            return Command.UNREGISTER;
        } else if ("UNROK".equals(string)) {
            return Command.UNREGISTER_OK;
        } else if ("JOIN".equals(string)) {
            return Command.JOIN;
        } else if ("JOINOK".equals(string)) {
            return Command.JOIN_OK;
        } else if ("LEAVE".equals(string)) {
            return Command.LEAVE;
        } else if ("LEAVEOK".equals(string)) {
            return Command.LEAVE_OK;
        } else if ("SER".equals(string)) {
            return Command.SEARCH;
        } else if ("SEROK".equals(string)) {
            return Command.SEARCH_OK;
        } else if ("GOSDIS".equals(string)) {
            return Command.NODE_DISCOVERED;
        } else if ("GOSUNR".equals(string)) {
            return Command.NODE_UNRESPONSIVE;
        } else if ("HEART".equals(string)) {
            return Command.HEART_BEAT;
        } else {
            return Command.UNKNOWN;
        }
    }
}
