package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.model.Trip;

public class HopSpeedSlow extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Very slow speed of %.2f kph over %.1fm on route %s trip %s " +
    		"stop sequence %d.";
    
    final float metersPerSecond;

    final float distance;
    
    final Trip trip;
    
    final int seq;
    
    public HopSpeedSlow(float metersPerSecond, float distance, Trip trip, int seq){
    	this.metersPerSecond = metersPerSecond;
    	this.distance = distance;
    	this.trip = trip;
    	this.seq = seq;
    }
    
    @Override
    public String getMessage() {
        double kph = metersPerSecond * 3.6;
        return String.format(FMT, kph, distance, trip.getRoute().getId(), trip.getId(), seq);
    }

}
