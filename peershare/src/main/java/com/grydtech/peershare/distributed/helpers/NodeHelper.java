package com.grydtech.peershare.distributed.helpers;

import com.grydtech.peershare.distributed.models.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class NodeHelper {

    private static final Random random = new Random();

    private NodeHelper() {
    }

    public static List<Node> getRandomNodes(List<Node> nodes) {
        List<Node> randomNodes = new ArrayList<>();

        int bound = nodes.size();

        if (bound == 1) {
            randomNodes.add(nodes.get(0));
        } else if (bound > 1) {
            int index1 = random.nextInt(bound);
            int index2 = random.nextInt(bound);

            while (index1 == index2) {
                index2 = random.nextInt(bound);
            }
            randomNodes.add(nodes.get(index1));
            randomNodes.add(nodes.get(index2));
        }

        return randomNodes;
    }
}
