package org.opentripplanner.graph_builder.impl.stopsAlerts;

import lombok.Setter;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Ben
 * Date: 06/01/13
 * Time: 11:47
 * To change this template use File | Settings | File Templates.
 */
public class TransitType extends AbstractStopTester {

    @Setter
    TraverseMode transitType;

    @Override
    public boolean fulfillDemands(TransitStop ts, Graph graph) {
        if (ts.getModes().contains(transitType))
            return true;
        return false;
    }
}
