package org.opentripplanner.ext.flex.flexpathcalculator;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.ext.flex.trip.ContinuousPickupDropOffTrip;
import org.opentripplanner.routing.graph.Vertex;

public class ContinuousStopsFlexPathCalculator implements FlexPathCalculator<Double> {

  private final ContinuousPickupDropOffTrip trip;

  public ContinuousStopsFlexPathCalculator(ContinuousPickupDropOffTrip trip) {
    this.trip = trip;
  }

  @Override
  public FlexPath calculateFlexPath(
      Vertex fromv, Vertex tov, Double fromStopIndex, Double toStopIndex
  ) {
    int distance = 0;
    int departureTime = trip.earliestDepartureTime(Integer.MIN_VALUE, fromStopIndex, toStopIndex, 0);
    int arrivalTime = trip.latestArrivalTime(Integer.MAX_VALUE, fromStopIndex, toStopIndex, 0);

    if (departureTime >= arrivalTime) return null;
    //TODO: Generate geometry from edges
    return new FlexPath(
            distance,
            arrivalTime - departureTime,
            GeometryUtils.makeLineString(fromv.getLon(), fromv.getLat(), tov.getLon(), tov.getLat())
    );
  }
}
