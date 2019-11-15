package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.Trip;

public class TripDuplicate implements DataImportIssue {

   public static final String FMT = "Possible GTFS feed error: Duplicate trip (skipping). " +
   		"New: %s Existing: %s";
   
   final Trip newTrip;
   final Trip existingTrip;
   
   public TripDuplicate(Trip newTrip, Trip existingTrip){
	   this.newTrip = newTrip;
	   this.existingTrip = existingTrip;
   }

   @Override
   public String getMessage() {
       return String.format(FMT, newTrip, existingTrip);
   }

}
