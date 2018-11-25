package com.grydtech.peershare.distributed.models;

import java.util.Date;
import java.util.UUID;

public class MessageInfo {

    private UUID messageId;
    private Date startTime;

    public MessageInfo(UUID messageId) {
        this.messageId = messageId;
        this.startTime = new Date();
    }

    public UUID getMessageId() {
        return messageId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public boolean isExpired(int timeOut) {
        Date now = new Date();

        return (now.getTime() - startTime.getTime()) > timeOut * 1000;
    }
}
