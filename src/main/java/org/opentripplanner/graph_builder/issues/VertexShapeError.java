package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.routing.graph.Edge;

public class VertexShapeError implements DataImportIssue {

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
