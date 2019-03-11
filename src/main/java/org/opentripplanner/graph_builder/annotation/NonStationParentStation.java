package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

public class NonStationParentStation extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Stop %s contains a parentStation (%s) with a location_type != 1.";
    
    final TransitStop stop;
    
    public NonStationParentStation(TransitStop stop){
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
