package com.grydtech.peershare.shared.services.impl;

import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.shared.models.SerializableMessage;
import com.grydtech.peershare.shared.services.UDPMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@Service
public class UDPMessageSenderImpl implements UDPMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCPMessageSenderImpl.class);

    private final DatagramSocket udpSocket;

    @Autowired
    public UDPMessageSenderImpl(DatagramSocket udpSocket) {
        this.udpSocket = udpSocket;
    }

    public void sendMessage(SerializableMessage message, Node destinationNode) throws IOException {
        String messageString = message.serialize();
        byte[] buf = messageString.getBytes();
        InetAddress address = InetAddress.getByName(destinationNode.getHost());
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, destinationNode.getUdpPort());

        udpSocket.send(packet);

        LOGGER.info("SHARED: request: \"{}\" sent to: \"{}\"", messageString, destinationNode.getId());
    }
}
