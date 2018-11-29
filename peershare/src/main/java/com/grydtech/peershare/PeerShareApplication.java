package com.grydtech.peershare;

import com.grydtech.peershare.distributed.DistributedClient;
import com.grydtech.peershare.files.services.FileStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class PeerShareApplication {

    private final DistributedClient distributedClient;
    private final FileStore fileStore;

    @Autowired
    public PeerShareApplication(DistributedClient DistributedClient, FileStore fileStore) {
        this.distributedClient = DistributedClient;
        this.fileStore = fileStore;
    }

    @EventListener
    public void afterApplicationReady(ApplicationReadyEvent event) {
        this.fileStore.index();
        this.distributedClient.start();
    }

    public static void main(String[] args) {
        SpringApplication.run(PeerShareApplication.class);
    }
}
