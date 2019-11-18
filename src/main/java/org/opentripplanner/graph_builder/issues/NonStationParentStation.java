package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

public class NonStationParentStation implements DataImportIssue {

    public static final String FMT = "Stop %s contains a parentStation (%s) with a location_type != 1.";
    
    final TransitStopVertex stop;
    
    public NonStationParentStation(TransitStopVertex stop){
    	this.stop = stop;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, stop, stop.getStop().getParentStation());
    }

    @Override
    public Vertex getReferencedVertex() {
        return this.stop;
    }
    
}
