package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.routing.graph.Edge;

public class ElevationFlattened extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Edge %s was steeper than Baldwin Street and was flattened.";
    
    final Edge edge;
    
    public ElevationFlattened(Edge edge){
    	this.edge = edge;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, edge);
    }

}
