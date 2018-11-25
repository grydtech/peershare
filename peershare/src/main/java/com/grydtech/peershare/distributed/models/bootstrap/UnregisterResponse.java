package com.grydtech.peershare.distributed.models.bootstrap;

import com.grydtech.peershare.distributed.models.Command;
import com.grydtech.peershare.shared.models.DeserializableMessage;

public class UnregisterResponse implements DeserializableMessage {

    private BootstrapResponseStatus status;

    public BootstrapResponseStatus getStatus() {
        return status;
    }

    @Override
    public void deserialize(String message) {
        String[] parts = message.split(" ");
        if (!Command.UNREGISTER_OK.toString().equals(parts[1])) return;

        this.status = BootstrapResponseStatus.byCode(Integer.parseInt(parts[2]));
    }
}
