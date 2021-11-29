package org.opentripplanner.graph_builder.module.stopsAlerts;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

public class TransitType extends AbstractStopTester {

    TraverseMode transitType;


    /**
     * @return true if a transit type of type transitType is pass through that stop
     */
    @Override
    public boolean fulfillDemands(TransitStopVertex ts, Graph graph) {
        if (ts.getModes().contains(transitType)) {
            return true;
        }
        return false;
    }
}
