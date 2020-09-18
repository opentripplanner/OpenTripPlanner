package org.opentripplanner.ext.flex.distancecalculator;

import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Calculate the driving times based on the shcheduled timetable for the route.
 */
public class ScheduledDistanceCalculator implements DistanceCalculator {
  private final DistanceCalculator distanceCalculator;
  private final FlexTrip trip;

  public ScheduledDistanceCalculator(DistanceCalculator distanceCalculator, FlexTrip trip) {
    this.distanceCalculator = distanceCalculator;
    this.trip = trip;
  }

  @Override
  public DistanceAndDuration getDuration(
      Vertex fromv, Vertex tov, int fromIndex, int toIndex
  ) {
    int distance = distanceCalculator.getDuration(fromv, tov, fromIndex, toIndex).distanceMeters;
    int departureTime = trip.earliestDepartureTime(Integer.MIN_VALUE, fromIndex, toIndex, 0);
    int arrivalTime = trip.latestArrivalTime(Integer.MAX_VALUE, fromIndex, toIndex, 0);

    if (departureTime >= arrivalTime) return null;
    return new DistanceAndDuration(distance, arrivalTime - departureTime);
  }
}
