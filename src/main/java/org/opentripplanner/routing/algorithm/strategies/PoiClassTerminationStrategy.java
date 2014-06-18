package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.PoiVertex;

public class PoiClassTerminationStrategy implements SearchTerminationStrategy{

    String category;

    public PoiClassTerminationStrategy(String category){
        this.category = category;
    }

    @Override
    public boolean shouldSearchContinue(Vertex origin, Vertex target, State current, ShortestPathTree spt, RoutingRequest traverseOptions) {
        Vertex currentVertex = current.getVertex();
        if (currentVertex instanceof PoiVertex){
            if (((PoiVertex) currentVertex).getCategories().contains(category)){
                traverseOptions.rctx.target = currentVertex;
                return false;
            }
        }
        return true;
    }
}
