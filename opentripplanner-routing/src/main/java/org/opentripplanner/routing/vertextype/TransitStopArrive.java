package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.graph.Graph;

public class TransitStopArrive extends OffboardVertex {

    private static final long serialVersionUID = 9213431651426739857L;

    public TransitStopArrive(Graph g, Stop stop) {
        super(g, GtfsLibrary.convertIdToString(stop.getId()) + "_arrive", stop);
    }

}
