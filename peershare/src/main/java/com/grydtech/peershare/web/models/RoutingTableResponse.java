package com.grydtech.peershare.web.models;

import com.grydtech.peershare.client.models.Node;

import java.util.List;

public class RoutingTableResponse {

    private final List<Node> table;

    public RoutingTableResponse(List<Node> table) {
        this.table = table;
    }

    public List<Node> getTable() {
        return table;
    }
}
