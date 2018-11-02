package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.model.Trip;

public class TripDegenerate extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

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
