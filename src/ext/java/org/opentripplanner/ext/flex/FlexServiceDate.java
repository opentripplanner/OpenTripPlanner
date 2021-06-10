package org.opentripplanner.ext.flex;

import gnu.trove.set.TIntSet;

import java.util.TimeZone;

import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graph.Graph;

/**
 * A lightweight wrapper around a serviceDate to hold a services index
 */
public class FlexServiceDate {

  /** The local date */
  public final ServiceDate serviceDate;

  /** Which services are running on the date.*/
  public final TIntSet servicesRunning;

  FlexServiceDate(ServiceDate serviceDate, TIntSet servicesRunning) {
    this.serviceDate = serviceDate;
    this.servicesRunning = servicesRunning;
  }

  boolean isFlexTripRunning(FlexTrip flexTrip, Graph graph) {
    return servicesRunning != null
        && servicesRunning.contains(graph.getServiceCodes().get(flexTrip.getTrip().getServiceId()));
  }  
  
  public int getAsEpochSeconds(TimeZone tz) {
	return (int)(serviceDate.getAsDate(tz).getTime()/1000);
  }
}