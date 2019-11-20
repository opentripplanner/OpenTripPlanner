package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.Trip;

public class TripDegenerate implements DataImportIssue {

    public static final String FMT = "Trip %s has fewer than two stops. " +
    		"We will not use it for routing. This is probably an error in your data";
    
    final Trip trip;
    
    public TripDegenerate(Trip trip){
    	this.trip = trip;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, trip);
    }

}
