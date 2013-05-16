package org.opentripplanner.graph_builder.impl.stopsAlerts;

import com.vividsolutions.jts.geom.Coordinate;
import lombok.Setter;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Ben
 * Date: 24/04/13
 * Time: 12:33
 * To change this template use File | Settings | File Templates.
 */
public class NumOfRoutes extends AbstractStopTester {

    @Setter
    int n;

    @Override
    public boolean fulfillDemands(TransitStop ts, Graph graph) {
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService.getRoutesForStop(ts.getStopId()).size() > n) return true;
        return false;
    }

}
