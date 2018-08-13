package org.opentripplanner.graph_builder.annotation;

import gnu.trove.list.TIntList;
import org.opentripplanner.model.Trip;

public class RepeatedStops extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Trip %s visits stops repeatedly. Removed duplicates at stop sequence numbers %s.";
    
    public final Trip trip;

    public final TIntList removedStopSequences;
    
    public RepeatedStops(Trip trip, TIntList removedStopSequences){
    	this.trip = trip;
        this.removedStopSequences = removedStopSequences;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, trip.getId(), removedStopSequences);
    }

}
