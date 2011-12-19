package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.edgetype.TripPattern;

public class PatternStopVertex extends OnboardVertex {

    private static final long serialVersionUID = 1L;
    
    public PatternStopVertex(String label, double x, double y, 
        AgencyAndId stopId, TripPattern tripPattern) {
        super(label, x, y, stopId, tripPattern);
    }
    
}
