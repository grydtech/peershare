package com.grydtech.peershare;

import com.grydtech.peershare.client.Client;
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

    @Autowired
    public PeerShareApplication(Client Client) {
        this.Client = Client;
    }

    @EventListener
    public void afterApplicationReady(ApplicationReadyEvent event) {
        this.Client.start();
    }

    public static void main(String[] args) {
        SpringApplication.run(PeerShareApplication.class);
    }
}
