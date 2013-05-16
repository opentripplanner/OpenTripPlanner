package org.opentripplanner.graph_builder.impl.stopsAlerts;

import com.vividsolutions.jts.geom.Coordinate;
import lombok.Setter;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Ben
 * Date: 24/04/13
 * Time: 09:58
 * To change this template use File | Settings | File Templates.
 */
public class NStopsNearBy extends AbstractStopTester {

    @Setter
    double r;

    @Setter
    int n;

    @Override
    public boolean fulfillDemands(TransitStop ts, Graph graph) {
        List<TransitStop> stopList = graph.streetIndex.getNearbyTransitStops(tsToCoor(ts),r);
        if(stopList.size() > n-1) return true;
        return false;
    }


    private Coordinate tsToCoor(TransitStop ts){
        return new Coordinate(ts.getX(),ts.getY());
    }
}
