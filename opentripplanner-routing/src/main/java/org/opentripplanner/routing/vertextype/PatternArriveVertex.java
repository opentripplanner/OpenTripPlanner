package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;

public class PatternArriveVertex extends PatternStopVertex {

    private static final long serialVersionUID = 4858000141204480555L;

    public PatternArriveVertex(Graph g, String label, double x, double y, 
            AgencyAndId stopId, TripPattern tripPattern) {
        super(g, label, x, y, stopId, tripPattern);
    }

}
