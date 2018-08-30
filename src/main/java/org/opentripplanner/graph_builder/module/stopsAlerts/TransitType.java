package org.opentripplanner.graph_builder.module.stopsAlerts;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;

public class TransitType extends AbstractStopTester {

    TraverseMode transitType;


    /**
     * @retrun return true if a transit type of type transitType is pass through that stop
     */
    @Override
    public boolean fulfillDemands(TransitStop ts, Graph graph) {
        if (ts.getModes().contains(transitType))
            return true;
        return false;
    }
}
