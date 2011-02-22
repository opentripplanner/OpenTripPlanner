package org.opentripplanner.routing.algorithm;

import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;

/**
 * When a temporary {@link Vertex} is generated at the origin or destination of a search, it is
 * often necessary to provide temporary edges linking the {@link Vertex} to the rest of the graph.
 * The extra edges strategy provides a plugin-interface for providing those connections.
 * 
 * @author bdferris
 * @see DefaultExtraEdgesStrategy
 */
public interface ExtraEdgesStrategy {

    /**
     * Add any incoming edges from vertices in the graph to the origin vertex in a backwards search.
     * 
     * @param extraEdges
     * @param origin
     */
    public void addIncomingEdgesForOrigin(Map<Vertex, List<Edge>> extraEdges, Vertex origin);

    /**
     * Add any incoming edges from the target vertex to vertices in the graph in a backwards search.
     * 
     * @param extraEdges
     * @param origin
     */
    public void addIncomingEdgesForTarget(Map<Vertex, List<Edge>> extraEdges, Vertex target);

    /**
     * Add any outgoing edges from the origin vertex to vertices in the graph in a forwards search.
     * 
     * @param extraEdges
     * @param origin
     */
    public void addOutgoingEdgesForOrigin(Map<Vertex, List<Edge>> extraEdges, Vertex origin);

    /**
     * Add any outgoing eges from vertices in the graph to the target vertex in a forward search.
     * 
     * @param extraEdges
     * @param target
     */
    public void addOutgoingEdgesForTarget(Map<Vertex, List<Edge>> extraEdges, Vertex target);
}
