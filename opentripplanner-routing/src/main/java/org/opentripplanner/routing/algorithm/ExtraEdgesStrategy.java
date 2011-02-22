package org.opentripplanner.routing.algorithm;

import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;

public interface ExtraEdgesStrategy {
    
    public Map<Vertex, List<Edge>> getIncomingExtraEdges(Vertex origin, Vertex target);
    
    public Map<Vertex, List<Edge>> getOutgoingExtraEdges(Vertex origin, Vertex target);
}
