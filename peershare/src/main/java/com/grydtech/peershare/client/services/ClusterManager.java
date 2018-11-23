package com.grydtech.peershare.client.services;

import com.grydtech.peershare.client.exceptions.BootstrapException;
import com.grydtech.peershare.client.exceptions.IllegalCommandException;
import com.grydtech.peershare.client.models.Node;
import com.grydtech.peershare.shared.services.Manager;
import io.reactivex.Observable;

import java.io.IOException;
import java.util.List;

public interface ClusterManager extends Manager {

    void register() throws IOException, BootstrapException;

    void unregister() throws IOException, BootstrapException, IllegalCommandException;

    void join() throws IllegalCommandException, IOException;

    void leave() throws IOException;

    void nodeDiscovered(Node discoveredNode, int hop) throws IOException;

    void nodeUnresponsive(Node unresponsiveNode, int hop) throws IOException;

    void nodeConnected(Node connectedNode) throws IOException;

    void nodeDisconnected(Node disconnectedNode);

    void nodeAlive(Node aliveNode) throws IOException;

    Observable<List<Node>> getConnectedCluster();
}
