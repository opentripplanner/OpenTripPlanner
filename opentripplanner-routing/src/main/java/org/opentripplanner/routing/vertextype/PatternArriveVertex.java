package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.edgetype.TripPattern;

public class PatternArriveVertex extends PatternStopVertex {

    private static final long serialVersionUID = 4858000141204480555L;

    public PatternArriveVertex(String label, double x, double y, 
            AgencyAndId stopId, TripPattern tripPattern) {
        super(label, x, y, stopId, tripPattern);
    }

}
