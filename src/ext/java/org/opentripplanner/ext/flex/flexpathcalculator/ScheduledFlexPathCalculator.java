package org.opentripplanner.ext.flex.flexpathcalculator;

import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Calculate the driving times based on the scheduled timetable for the route.
 */
public class ScheduledFlexPathCalculator implements FlexPathCalculator {

  private final FlexPathCalculator flexPathCalculator;
  private final FlexTrip trip;

  public ScheduledFlexPathCalculator(FlexPathCalculator flexPathCalculator, FlexTrip<?, ?> trip) {
    this.flexPathCalculator = flexPathCalculator;
    this.trip = trip;
  }

  @Override
  public FlexPath calculateFlexPath(
    Vertex fromv,
    Vertex tov,
    int boardStopPosition,
    int alightStopPosition
  ) {
    final var flexPath = flexPathCalculator.calculateFlexPath(
      fromv,
      tov,
      boardStopPosition,
      alightStopPosition
    );
    if (flexPath == null) {
      return null;
    }
    int departureTime = trip.earliestDepartureTime(
      Integer.MIN_VALUE,
      boardStopPosition,
      alightStopPosition,
      0
    );

    if (departureTime == MISSING_VALUE) {
      return null;
    }

    int arrivalTime = trip.latestArrivalTime(
      Integer.MAX_VALUE,
      boardStopPosition,
      alightStopPosition,
      0
    );

    if (arrivalTime == MISSING_VALUE) {
      return null;
    }

    if (departureTime >= arrivalTime) {
      return null;
    }
    return new FlexPath(
      flexPath.distanceMeters,
      arrivalTime - departureTime,
      flexPath::getGeometry
    );
  }
}
