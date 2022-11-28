package org.opentripplanner.ext.flex;

import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.RegularStop;

public class FlexAccessEgress {

  public final RegularStop stop;
  public final int preFlexTime;
  public final int flexTime;
  public final int postFlexTime;
  private final int fromStopIndex;
  private final int toStopIndex;
  private final int differenceFromStartOfTime;
  private final FlexTrip trip;
  public final State lastState;
  public final boolean directToStop;

  public FlexAccessEgress(
    RegularStop stop,
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
    this.stop = stop;
    this.preFlexTime = preFlexTime;
    this.flexTime = flexTime;
    this.postFlexTime = postFlexTime;
    this.fromStopIndex = fromStopIndex;
    this.toStopIndex = toStopIndex;
    this.differenceFromStartOfTime = differenceFromStartOfTime;
    this.trip = trip;
    this.lastState = lastState;
    this.directToStop = directToStop;
  }

  public int earliestDepartureTime(int departureTime) {
    int requestedTransitDepartureTime = departureTime + preFlexTime - differenceFromStartOfTime;
    int earliestAvailableTransitDepartureTime = trip.earliestDepartureTime(
      requestedTransitDepartureTime,
      fromStopIndex,
      toStopIndex,
      flexTime
    );
    if (earliestAvailableTransitDepartureTime == -1) {
      return -1;
    }
    return earliestAvailableTransitDepartureTime - preFlexTime + differenceFromStartOfTime;
  }

  public int latestArrivalTime(int arrivalTime) {
    int requestedTransitArrivalTime = arrivalTime - postFlexTime - differenceFromStartOfTime;
    int latestAvailableTransitArrivalTime = trip.latestArrivalTime(
      requestedTransitArrivalTime,
      fromStopIndex,
      toStopIndex,
      flexTime
    );
    if (latestAvailableTransitArrivalTime == -1) {
      return -1;
    }
    return latestAvailableTransitArrivalTime + postFlexTime + differenceFromStartOfTime;
  }
}
