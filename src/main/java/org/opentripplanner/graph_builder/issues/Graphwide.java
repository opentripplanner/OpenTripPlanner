package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;

public class Graphwide implements DataImportIssue {

    String message;
    
    public Graphwide(String message){
    	this.message = message;
    }

    @Override
    public String getMessage() {
        return String.format("graph-wide: " + message);
    }
    
}