package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.Trip;

public class TripOvertaking implements DataImportIssue {

    public static final String FMT = "Possible GTFS feed error: Trip %s overtakes trip %s " +
    		"(which has the same stops) at stop index %d. " +
    		"This will be handled correctly but inefficiently.";
    
    final Trip overtaker;
    final Trip overtaken;
    int index;
    
    public TripOvertaking(Trip overtaker, Trip overtaken, int index){
    	this.overtaker = overtaker;
    	this.overtaken = overtaken;
    	this.index = index;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, overtaker, overtaken, index);
    }

}
