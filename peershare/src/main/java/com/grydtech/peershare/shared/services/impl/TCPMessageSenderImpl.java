package com.grydtech.peershare.shared.services.impl;

import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.shared.models.SerializableMessage;
import com.grydtech.peershare.shared.services.TCPMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;

@Service
public class TCPMessageSenderImpl implements TCPMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCPMessageSenderImpl.class);

    @Override
    public String sendMessage(SerializableMessage message, Node destinationNode) throws IOException {
        Socket socket = new Socket(destinationNode.getHost(), destinationNode.getPort());
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String messageString = message.serialize();

        LOGGER.info("request: \"{}\" sent to: \"{}\"", messageString, destinationNode.getId());

        pw.println(messageString);

        char[] data = new char[1000];
        int count = br.read(data);

        String response = String.valueOf(data, 0, count);

        LOGGER.info("response: \"{}\" received from: \"{}\"", response, destinationNode.getId());

        return response;
    }
}
