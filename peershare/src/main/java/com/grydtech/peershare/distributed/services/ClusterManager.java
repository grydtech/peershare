package com.grydtech.peershare.distributed.services;

import com.grydtech.peershare.distributed.exceptions.BootstrapException;
import com.grydtech.peershare.distributed.exceptions.IllegalCommandException;
import com.grydtech.peershare.distributed.models.Node;
import com.grydtech.peershare.shared.services.Manager;
import io.reactivex.Observable;

import java.io.IOException;
import java.util.List;

public interface ClusterManager extends Manager {

    void register() throws IOException, BootstrapException;

    void unregister() throws IOException, BootstrapException, IllegalCommandException;

    void join() throws IllegalCommandException, IOException;

    void leave() throws IOException;

    void nodeConnected(Node connectedNode) throws IOException;

    void nodeDisconnected(Node disconnectedNode);

    void nodeDiscovered(Node discoveredNode) throws IOException;

    void nodeReset(Node aliveNode) throws IOException;

    Observable<List<Node>> getConnectedClusterObservable();

    List<Node> getConnectedCluster();
}
