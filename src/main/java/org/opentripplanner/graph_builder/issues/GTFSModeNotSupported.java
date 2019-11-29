package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.Trip;

public class GTFSModeNotSupported implements DataImportIssue {

   public static final String FMT = "Trip %s has a mode %s which is not supported.";

   final Trip trip;

   final String gtfsMode;

   public GTFSModeNotSupported(Trip trip, String gtfsMode){
      this.trip = trip;
      this.gtfsMode = gtfsMode;
   }
   
   @Override
   public String getMessage() {
       return String.format(FMT, trip, trip.getServiceId());
   }

}
