package org.opentripplanner.routing.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.HasEdges;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.location.StreetLocation;

public class GraphLibrary {

    public static Collection<Edge> getIncomingEdges(Graph graph, Vertex tov,
            Map<Vertex, List<Edge>> extraEdges) {

        Collection<Edge> incoming = null;

        if (tov instanceof HasEdges) {
            incoming = extendEdges(incoming, ((HasEdges) tov).getIncoming());
        } else {
            GraphVertex gv = graph.getGraphVertex(tov);
            if (gv != null)
                incoming = extendEdges(incoming, gv.getIncoming());
        }

        if (extraEdges != null && extraEdges.containsKey(tov))
            incoming = extendEdges(incoming, extraEdges.get(tov));

        if (incoming == null)
            incoming = Collections.emptyList();

        return incoming;
    }

    public static Collection<Edge> getOutgoingEdges(Graph graph, Vertex fromv,
            Map<Vertex, List<Edge>> extraEdges) {

        Collection<Edge> outgoing = null;

        if (fromv instanceof HasEdges) {
            outgoing = extendEdges(outgoing, ((HasEdges) fromv).getOutgoing());
        } else {
            GraphVertex gv = graph.getGraphVertex(fromv);
            if (gv != null)
                outgoing = extendEdges(outgoing, gv.getOutgoing());
        }

        if (extraEdges != null && extraEdges.containsKey(fromv))
            outgoing = extendEdges(outgoing, extraEdges.get(fromv));

        if (fromv instanceof StreetLocation) {
            StreetLocation sl = (StreetLocation) fromv;
            outgoing = extendEdges(outgoing, sl.getExtra());
        }

        if (outgoing == null)
            outgoing = Collections.emptyList();

        return outgoing;
    }

    /****
     * Private Methods
     ****/

    private static <E extends Edge> Collection<Edge> extendEdges(Collection<Edge> existing,
            Collection<E> additionalEdges) {

        if (existing == null || existing.size() == 0) {
            if (additionalEdges == null || additionalEdges.isEmpty())
                return null;
            return new ArrayList<Edge>(additionalEdges);
        }

        if (additionalEdges == null || additionalEdges.size() == 0)
            return existing;

        List<Edge> edges = new ArrayList<Edge>(existing.size() + additionalEdges.size());
        edges.addAll(existing);
        edges.addAll(additionalEdges);
        return edges;
    }
}
