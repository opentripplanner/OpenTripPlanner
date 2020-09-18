package org.opentripplanner.ext.flex;

import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;
import org.opentripplanner.routing.core.State;

public class FlexAccessEgress extends AccessEgress {

  private final int preFlexTime;
  private final int flexTime;
  private final int postFlexTime;
  private final int fromStopIndex;
  private final int toStopIndex;
  private final int differenceFromStartOfTime;
  private final FlexTrip trip;
  private final boolean directToStop;

  public FlexAccessEgress(
      int toFromStop,
      int preFlexTime,
      int flexTime,
      int postFlexTime,
      int fromStopIndex,
      int toStopIndex,
      int differenceFromStartOfTime,
      FlexTrip trip,
      State lastState,
      boolean directToStop
  ) {
    super(toFromStop, preFlexTime + flexTime + postFlexTime, lastState);
    this.preFlexTime = preFlexTime;
    this.flexTime = flexTime;
    this.postFlexTime = postFlexTime;
    this.fromStopIndex = fromStopIndex;
    this.toStopIndex = toStopIndex;
    this.differenceFromStartOfTime = differenceFromStartOfTime;
    this.trip = trip;
    this.directToStop = directToStop;
  }

  @Override
  public int earliestDepartureTime(int departureTime) {
    int requestedTransitDepartureTime = departureTime + preFlexTime - differenceFromStartOfTime;
    int earliestAvailableTransitDepartureTime = trip.earliestDepartureTime(
        requestedTransitDepartureTime,
        fromStopIndex,
        toStopIndex,
        flexTime
    );
    if (earliestAvailableTransitDepartureTime == -1) { return -1; }
    return earliestAvailableTransitDepartureTime - preFlexTime + differenceFromStartOfTime;
  }

  @Override
  public int latestArrivalTime(int arrivalTime) {
    int requestedTransitArrivalTime = arrivalTime - postFlexTime - differenceFromStartOfTime;
    int latestAvailableTransitArrivalTime = trip.latestArrivalTime(
        requestedTransitArrivalTime,
        fromStopIndex,
        toStopIndex,
        flexTime
    );
    if (latestAvailableTransitArrivalTime == -1) { return -1; }
    return latestAvailableTransitArrivalTime + postFlexTime + differenceFromStartOfTime;
  }

  @Override
  public int numberOfLegs() {
    return directToStop ? 2 : 3;
  }

  @Override
  public boolean stopReachedOnBoard() {
    return directToStop;
  }
}
