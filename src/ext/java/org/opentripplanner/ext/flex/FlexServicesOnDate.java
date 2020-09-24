package org.opentripplanner.ext.flex;

import gnu.trove.set.TIntSet;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.util.stream.Stream;

class FlexServicesOnDate {
  public final TIntSet servicesRunning;
  public final int secondsFromStartOfTime;
  public final ServiceDate serviceDate;

  FlexServicesOnDate(
      TIntSet servicesRunning, int secondsFromStartOfTime, ServiceDate serviceDate
  ) {
    this.servicesRunning = servicesRunning;
    this.secondsFromStartOfTime = secondsFromStartOfTime;
    this.serviceDate = serviceDate;
  }

  boolean isFlexTripRunning(FlexTrip flexTrip, Graph graph) {
    return servicesRunning.contains(graph.getServiceCodes().get(flexTrip.getTrip().getServiceId()));
  }

  Stream<FlexAccessTemplate> getFlexAccessTemplates(
      NearbyStop stop, FlexTrip trip, FlexPathCalculator flexPathCalculator
  ) {
    // TODO: Optimization: Could we filter here if earliestDepartureTime or latestArrivalTime is -1
    return trip.getFlexAccessTemplates(
        stop,
        secondsFromStartOfTime,
        serviceDate,
        flexPathCalculator
    );
  }

  Stream<FlexEgressTemplate> getFlexEgressTemplates(
      NearbyStop stop, FlexTrip trip, FlexPathCalculator flexPathCalculator
  ) {
    // TODO: Optimization: Could we filter here if earliestDepartureTime or latestArrivalTime is -1
    return trip.getFlexEgressTemplates(
        stop,
        secondsFromStartOfTime,
        serviceDate,
        flexPathCalculator
    );
  }
}