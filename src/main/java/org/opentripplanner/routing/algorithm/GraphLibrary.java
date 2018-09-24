package org.opentripplanner.routing.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

public class GraphLibrary {

    public static Collection<Edge> getIncomingEdges(Graph graph, Vertex tov,
            Map<Vertex, List<Edge>> extraEdges) {

        if (extraEdges.containsKey(tov)) {
            Collection<Edge> ret = new ArrayList<Edge>(tov.getIncoming());
            ret.addAll(extraEdges.get(tov));
            return ret;
        } else {
            return tov.getIncoming();
        }

    }

    public static Collection<Edge> getOutgoingEdges(Graph graph, Vertex fromv,
            Map<Vertex, List<Edge>> extraEdges) {

        if (extraEdges.containsKey(fromv)) {
            Collection<Edge> ret = new ArrayList<Edge>(fromv.getOutgoing());
            ret.addAll(extraEdges.get(fromv));
            return ret;
        } else {
            return fromv.getOutgoing();
        }
    }

}
