package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.model.Trip;

public class TripUndefinedService extends GraphBuilderAnnotation {

   private static final long serialVersionUID = 1L;

   public static final String FMT = "Trip %s references serviceId %s that was not defined in the feed.";
   
   final Trip trip;
   
   public TripUndefinedService(Trip trip){
	   this.trip = trip;
   }
   
   @Override
   public String getMessage() {
       return String.format(FMT, trip, trip.getServiceId());
   }

}
