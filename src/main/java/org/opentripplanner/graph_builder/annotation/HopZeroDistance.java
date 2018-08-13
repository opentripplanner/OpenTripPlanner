package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.model.Trip;

public class HopZeroDistance extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Zero-distance hop in %d seconds on trip %s stop sequence %d.";
    
    final int sec;
    final Trip trip;
    final int seq;
    
    HopZeroDistance(int sec, Trip trip, int seq){
    	this.sec = sec;
    	this.trip = trip;
    	this.seq = seq;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, sec, trip, seq);
    }

}
