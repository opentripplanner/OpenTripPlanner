package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.routing.graph.Edge;

public class BogusEdgeGeometry extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Edge %s has bogus geometry (some coordinates are NaN, " +
    		"or geometry has fewer than two points)";
    
    final Edge edge;
    
    public BogusEdgeGeometry(Edge edge){
    	this.edge = edge;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, edge);
    }

}
