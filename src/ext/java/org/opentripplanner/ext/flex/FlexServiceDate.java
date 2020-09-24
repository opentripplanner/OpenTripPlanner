package org.opentripplanner.ext.flex;

import gnu.trove.set.TIntSet;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graph.Graph;

public class FlexServiceDate {
  public final ServiceDate serviceDate;
  public final int secondsFromStartOfTime;
  public final TIntSet servicesRunning;

  FlexServiceDate(
      ServiceDate serviceDate, int secondsFromStartOfTime, TIntSet servicesRunning
  ) {
    this.serviceDate = serviceDate;
    this.secondsFromStartOfTime = secondsFromStartOfTime;
    this.servicesRunning = servicesRunning;
  }

  boolean isFlexTripRunning(FlexTrip flexTrip, Graph graph) {
    return servicesRunning.contains(graph.getServiceCodes().get(flexTrip.getTrip().getServiceId()));
  }
}