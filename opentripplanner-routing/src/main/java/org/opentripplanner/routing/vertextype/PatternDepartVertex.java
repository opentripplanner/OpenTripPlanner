package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;

public class PatternDepartVertex extends PatternStopVertex {

    private static final long serialVersionUID = -1458696615022789418L;

    public PatternDepartVertex(Graph g, String label, double x, double y, 
            AgencyAndId stopId, TripPattern tripPattern) {
        super(g, label, x, y, stopId, tripPattern);
    }

}
