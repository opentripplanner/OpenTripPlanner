package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.routing.graph.Edge;

public class VertexShapeError extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Transit edge %s has shape geometry which is far from its " +
    		"start/end vertices. This could be caused by bad shape geometry, or by incorrect " +
    		"use of defaultAgencyId.";
    
    final Edge edge;
    
    public VertexShapeError(Edge edge){
    	this.edge = edge;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, edge);
    }

}
