package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.routing.graph.Edge;

public class BogusEdgeGeometry implements DataImportIssue {

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
