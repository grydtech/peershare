package com.grydtech.peershare;

import com.grydtech.peershare.client.Client;
import com.grydtech.peershare.files.services.FileStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class PeerShareApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeerShareApplication.class);

    private final Client Client;
    private final FileStore fileStore;

    @Autowired
    public PeerShareApplication(Client Client, FileStore fileStore) {
        this.Client = Client;
        this.fileStore = fileStore;
    }

    @EventListener
    public void afterApplicationReady(ApplicationReadyEvent event) {
        this.fileStore.index();
        this.Client.start();
    }

    public static void main(String[] args) {
        SpringApplication.run(PeerShareApplication.class);
    }
}
