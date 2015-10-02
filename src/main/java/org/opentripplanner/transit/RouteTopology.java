package org.opentripplanner.transit;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This represents the topology of a given GTFS route, with branches and common trunks etc.
 * All trips on this route are interpreted as valid paths through a directed graph.
 * We then perform a topological sort on the stops, such that all edges point to the right on the
 * sorted stop list. See:
 * http://stackoverflow.com/a/17202968/778449
 * http://stackoverflow.com/a/28531695/778449
 *
 * All routes are cyclic because they contain multiple directions. So we must group trips by direction.
 * Should we perhaps be working on the dual graph (of from-to pairs)?
 * We could also keep track of which trips use each inter-stop segment.
 */
public class RouteTopology {

    private static Logger LOG = LoggerFactory.getLogger(RouteTopology.class);

    String routeId;
    int directionId;
    List<StopNode> sortedNodes = new ArrayList<>();
    TIntObjectMap<StopNode> nodeForStopIndex = new TIntObjectHashMap<>();

    public RouteTopology(String routeId, int directionId, Collection<TripPattern> patterns) {
        this.routeId = routeId;
        this.directionId = directionId;
        for (TripPattern pattern : patterns) {
            addPattern(pattern.stops);
        }
        try {
            topologicalSort();
        } catch (CyclicGraphException ex) {
            LOG.error("Route was cyclic.");
        }
    }

    private static class StopNode {
        public int stopIndex;
        public boolean visiting = false;
        Set<StopNode> outgoing = new HashSet<>();
        Set<StopNode> incoming = new HashSet<>();
        public StopNode (int stopIndex) {
            this.stopIndex = stopIndex;
        }
        public int degreeOut () { return outgoing.size(); }
        public int degreeIn () { return incoming.size(); }
    }

    public StopNode getNode (int stopIndex) {
        StopNode node = nodeForStopIndex.get(stopIndex);
        if (node == null) {
            node = new StopNode(stopIndex);
            nodeForStopIndex.put(stopIndex, node);
        }
        return node;
    }

    public void addPattern (int[] stopSequence) {
        for (int i = 0; i < stopSequence.length - 1; i++) {
            StopNode fromNode = getNode(stopSequence[i]);
            StopNode toNode = getNode(stopSequence[i + 1]);
            fromNode.outgoing.add(toNode);
            toNode.incoming.add(fromNode);
        }
    }

    public void topologicalSort() throws CyclicGraphException {
        Set<StopNode> visited = new HashSet<>();
        for (StopNode node : nodeForStopIndex.valueCollection()) {
            if (!visited.contains(node)) {
                visit(node, visited);
            }
        }
        Collections.reverse(sortedNodes);
    }

    public void visit(StopNode node, Set<StopNode> visited) throws CyclicGraphException {
        if (node.visiting) {
            throw new CyclicGraphException();
        } else {
            node.visiting = true;
            for (StopNode m : node.outgoing) {
                if (!visited.contains(m)) {
                    visit(m, visited);
                }
            }
            node.visiting = false;
            visited.add(node);
            sortedNodes.add(node);
        }
    }

    public void print () {
        System.out.println("Topology of route " + routeId + " direction " + directionId);
        for (StopNode node : sortedNodes) {
            if (node.degreeIn() == 0) {
                System.out.println("X");
            } else if (node.degreeIn() == 1) {
                System.out.println("|");
            } else if (node.degreeIn() > 1) {
                System.out.println("V");
            }
            System.out.print("O " + node.stopIndex + " to: ");
            for (StopNode m: node.outgoing) {
                System.out.print(m.stopIndex + " ");
            }
            System.out.println();
            if (node.degreeOut() == 0) {
                System.out.println("X");
            } else if (node.degreeOut() == 1) {
                System.out.println("|");
            } else if (node.degreeOut() > 1) {
                System.out.println("^");
            }
        }
    }

    public static class CyclicGraphException extends Exception {

    }

}
