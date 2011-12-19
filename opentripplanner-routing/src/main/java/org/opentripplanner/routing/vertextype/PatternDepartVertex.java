package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.edgetype.TripPattern;

public class PatternDepartVertex extends PatternStopVertex {

    private static final long serialVersionUID = -1458696615022789418L;

    public PatternDepartVertex(String label, double x, double y, 
            AgencyAndId stopId, TripPattern tripPattern) {
        super(label, x, y, stopId, tripPattern);
    }

}
