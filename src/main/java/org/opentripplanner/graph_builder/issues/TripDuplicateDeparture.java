package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.Trip;

public class TripDuplicateDeparture implements DataImportIssue {

    public static final String FMT = "Possible GTFS feed error: Duplicate first departure time. " +
    		"New trip: %s Existing trip: %s This will be handled correctly but inefficiently.";
    
    final Trip newTrip;
    final Trip existingTrip;
    
    public TripDuplicateDeparture(Trip newTrip, Trip existingTrip){
    	this.newTrip = newTrip;
    	this.existingTrip = existingTrip;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, newTrip, existingTrip);
    }

}
