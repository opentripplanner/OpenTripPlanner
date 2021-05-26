package org.opentripplanner.ext.flex;

import gnu.trove.set.TIntSet;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graph.Graph;

/**
 * This class contains information used in a flex router, and depends on the date the search was
 * made on.
 */
public class FlexServiceDate {

  /** The local date */
  public final ServiceDate serviceDate;

  /** Which services are running on the date.*/
  public final TIntSet servicesRunning;

  FlexServiceDate(
      ServiceDate serviceDate, TIntSet servicesRunning
  ) {
    this.serviceDate = serviceDate;
    this.servicesRunning = servicesRunning;
  }

  boolean isFlexTripRunning(FlexTrip flexTrip, Graph graph) {
    return servicesRunning != null
        && servicesRunning.contains(graph.getServiceCodes().get(flexTrip.getTrip().getServiceId()));
  }  
}