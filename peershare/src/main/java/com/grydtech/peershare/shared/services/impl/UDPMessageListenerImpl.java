package com.grydtech.peershare.shared.services.impl;

import com.grydtech.peershare.shared.services.UDPMessageListener;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class UDPMessageListenerImpl implements UDPMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UDPMessageListener.class);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final BehaviorSubject<String> messages = BehaviorSubject.create();

    private final DatagramSocket udpSocket;

    public UDPMessageListenerImpl(DatagramSocket udpSocket) {
        this.udpSocket = udpSocket;
    }

    @Override
    public Observable<String> listen() {
        executorService.submit(() -> {
            LOGGER.info("message acceptor started");

            while (!udpSocket.isClosed()) {
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                try {
                    udpSocket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String received = new String(packet.getData(), 0, packet.getLength());

                LOGGER.info("UDP packet received: \"{}\"", received);

                messages.onNext(received);
            }
        });

        return messages;
    }


}
