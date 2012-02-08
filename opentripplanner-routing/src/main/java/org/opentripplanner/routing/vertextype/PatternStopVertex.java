package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;

public class PatternStopVertex extends OnboardVertex {

    private static final long serialVersionUID = 1L;
    
    public PatternStopVertex(Graph g, String label, TripPattern tripPattern, Stop stop) {
        super(g, label, tripPattern, stop);
    }
        
}
