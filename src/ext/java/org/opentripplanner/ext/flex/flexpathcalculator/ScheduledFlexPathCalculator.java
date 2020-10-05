package org.opentripplanner.ext.flex.flexpathcalculator;

import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Calculate the driving times based on the shcheduled timetable for the route.
 */
public class ScheduledFlexPathCalculator implements FlexPathCalculator<Integer> {
  private final FlexPathCalculator<Integer> flexPathCalculator;
  private final FlexTrip<Integer> trip;

  public ScheduledFlexPathCalculator(FlexPathCalculator<Integer> flexPathCalculator, FlexTrip<Integer> trip) {
    this.flexPathCalculator = flexPathCalculator;
    this.trip = trip;
  }

  @Override
  public FlexPath calculateFlexPath(
      Vertex fromv, Vertex tov, Integer fromStopIndex, Integer toStopIndex
  ) {
    FlexPath flexPath = flexPathCalculator.calculateFlexPath(
        fromv,
        tov,
        fromStopIndex,
        toStopIndex
    );
    if (flexPath == null) { return null; }
    int distance = flexPath.distanceMeters;
    int departureTime = trip.earliestDepartureTime(Integer.MIN_VALUE, fromStopIndex, toStopIndex, 0);
    int arrivalTime = trip.latestArrivalTime(Integer.MAX_VALUE, fromStopIndex, toStopIndex, 0);

    if (departureTime >= arrivalTime) return null;
    return new FlexPath(distance, arrivalTime - departureTime);
  }
}
