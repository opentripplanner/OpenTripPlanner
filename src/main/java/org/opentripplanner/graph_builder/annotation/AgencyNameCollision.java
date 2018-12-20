package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.model.Agency;

public class AgencyNameCollision extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Agency %s was already defined by %s. Both feeds will refer " +
    		"to the same agency. Is this intentional?";
    
    final Agency agency;
    final String prevFeed;
    
    AgencyNameCollision(Agency agency, String prevFeed){
    	this.agency = agency;
    	this.prevFeed = prevFeed;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, agency, prevFeed);
    }

}
