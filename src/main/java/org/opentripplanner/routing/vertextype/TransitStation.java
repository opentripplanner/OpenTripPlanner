package org.opentripplanner.routing.vertextype;

import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.graph.Graph;

public class TransitStation extends TransitStationStop {
    private static final long serialVersionUID = 1L;

    public TransitStation(Graph graph, Stop stop) {
        super(graph, stop);
    }
}
