package com.grydtech.peershare.distributed.models.peer;

import com.grydtech.peershare.distributed.models.Command;
import com.grydtech.peershare.shared.models.DeserializableMessage;
import com.grydtech.peershare.shared.models.Message;
import com.grydtech.peershare.shared.models.SerializableMessage;

import java.util.UUID;

public class PeerLeaveResponse extends Message implements SerializableMessage, DeserializableMessage {

    private PeerResponseStatus status;

    public PeerLeaveResponse() {
    }

    public PeerLeaveResponse(PeerResponseStatus status, UUID messageId) {
        this.status = status;
        this.messageId = messageId;
    }

    public PeerResponseStatus getStatus() {
        return status;
    }

    @Override
    public void deserialize(String message) {
        String[] parts = message.split(" ");
        if (!Command.LEAVE_OK.toString().equals(parts[2])) return;

        this.messageId = UUID.fromString(parts[1]);
        int code = Integer.parseInt(parts[3]);
        this.status = PeerResponseStatus.byCode(code);
    }

    @Override
    public String serialize() {
        String s = String.format("%s %s %d", this.messageId.toString(), Command.LEAVE_OK.toString(), this.status.getCode());
        return String.format("%04d %s", s.length() + 5, s);
    }
}
