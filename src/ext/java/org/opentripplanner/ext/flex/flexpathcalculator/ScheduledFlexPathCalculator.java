package org.opentripplanner.ext.flex.flexpathcalculator;

import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Calculate the driving times based on the scheduled timetable for the route.
 */
public class ScheduledFlexPathCalculator implements FlexPathCalculator {

  private final FlexPathCalculator flexPathCalculator;
  private final FlexTrip trip;

  public ScheduledFlexPathCalculator(FlexPathCalculator flexPathCalculator, FlexTrip trip) {
    this.flexPathCalculator = flexPathCalculator;
    this.trip = trip;
  }

  @Override
  public FlexPath calculateFlexPath(Vertex fromv, Vertex tov, int fromStopIndex, int toStopIndex) {
    FlexPath flexPath = flexPathCalculator.calculateFlexPath(
      fromv,
      tov,
      fromStopIndex,
      toStopIndex
    );
    if (flexPath == null) {
      return null;
    }
    int distance = flexPath.distanceMeters;
    int departureTime = trip.earliestDepartureTime(
      Integer.MIN_VALUE,
      fromStopIndex,
      toStopIndex,
      0
    );
    int arrivalTime = trip.latestArrivalTime(Integer.MAX_VALUE, fromStopIndex, toStopIndex, 0);

    if (departureTime >= arrivalTime) {
      return null;
    }
    return new FlexPath(distance, arrivalTime - departureTime, flexPath::getGeometry);
  }
}
