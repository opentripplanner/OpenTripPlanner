package org.opentripplanner.ext.flex;

import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;

public class FlexAccessEgress {
  public final Stop stop;
  public final int preFlexTime;
  public final int postFlexTime;
  private final int fromStopIndex;
  private final int toStopIndex;
  private final FlexTrip trip;
  public final State lastState;
  public final boolean directToStop;

  public FlexAccessEgress(
      Stop stop,
      int[] flexTimes, // pre, flex, post
      int fromStopIndex,
      int toStopIndex,
      FlexTrip trip,
      State lastState,
      boolean directToStop
  ) {
    this.stop = stop;
    this.preFlexTime = flexTimes[0];
    this.postFlexTime = flexTimes[2];
    this.fromStopIndex = fromStopIndex;
    this.toStopIndex = toStopIndex;
    this.trip = trip;
    this.lastState = lastState;
    this.directToStop = directToStop;
  }

  public int getSafeTotalTime() {
	  State s = this.lastState;
	  Edge e = this.lastState.backEdge;
	  while(s != null && !(e instanceof FlexTripEdge)) {
		  e = s.backEdge;
		  s = s.getBackState();
	  }
	  
	  return this.trip.getSafeTotalTime(((FlexTripEdge)e).flexPath, this.fromStopIndex, this.toStopIndex);
  }
  
  public int earliestDepartureTime(int departureTime) {
    int requestedTransitDepartureTime = departureTime + preFlexTime;  
    int earliestAvailableTransitDepartureTime = trip.earliestDepartureTime(
        requestedTransitDepartureTime,
        fromStopIndex,
        toStopIndex
    );
    if (earliestAvailableTransitDepartureTime == -1) { return -1; }
    return earliestAvailableTransitDepartureTime - preFlexTime;
  }

  public int latestArrivalTime(int arrivalTime) {
    int requestedTransitArrivalTime = arrivalTime - postFlexTime;
    int latestAvailableTransitArrivalTime = trip.latestArrivalTime(
        requestedTransitArrivalTime,
        fromStopIndex,
        toStopIndex
    );
    if (latestAvailableTransitArrivalTime == -1) { return -1; }
    return latestAvailableTransitArrivalTime + postFlexTime;
  }
  
}
