package org.opentripplanner.ext.flex;

import java.util.TimeZone;

import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;

public class FlexAccessEgress {
  public final Stop stop;
  private final int fromStopIndex;
  private final int toStopIndex;
  private final FlexServiceDate serviceDate;
  private final FlexTrip trip;
  public final State lastState;
  public final boolean directToStop;

  public FlexAccessEgress(
      Stop stop,
      int fromStopIndex,
      int toStopIndex,
      FlexServiceDate serviceDate,
      FlexTrip trip,
      State lastState,
      boolean directToStop
  ) {
    this.stop = stop;
    this.fromStopIndex = fromStopIndex;
    this.toStopIndex = toStopIndex;
    this.serviceDate = serviceDate;
    this.trip = trip;
    this.lastState = lastState;
    this.directToStop = directToStop;
  }

  public int getSafeTotalTime() {
	  // I can't tell, is this a hack? (FIXME?)
	  State s = this.lastState;
	  Edge e = this.lastState.backEdge;
	  while(s != null && !(e instanceof FlexTripEdge)) {
		  e = s.backEdge;
		  s = s.getBackState();
	  }
	  
	  return this.trip.getSafeTotalTime(((FlexTripEdge)e).flexPath, this.fromStopIndex, this.toStopIndex);
  }
  
  public int earliestDepartureTime(int departureTime) {
	TimeZone tz = this.lastState.getOptions().getRoutingContext().graph.getTimeZone();
	long serviceDateAsEpoch = this.serviceDate.serviceDate.getAsDate(tz).getTime();  
		  
	long differenceFromStartOfTime = lastState.getTimeSeconds() - serviceDateAsEpoch/1000;
    int requestedTransitDepartureTime = departureTime + (int)differenceFromStartOfTime;
    int earliestAvailableTransitDepartureTime = trip.earliestDepartureTime(
        requestedTransitDepartureTime,
        fromStopIndex,
        toStopIndex
    );
    if (earliestAvailableTransitDepartureTime == -1) { return -1; }
    return earliestAvailableTransitDepartureTime - (int)differenceFromStartOfTime;
  }

  public int latestArrivalTime(int arrivalTime) {
	TimeZone tz = this.lastState.getOptions().getRoutingContext().graph.getTimeZone();
	long serviceDateAsEpoch = this.serviceDate.serviceDate.getAsDate(tz).getTime();  
	  
	long differenceFromStartOfTime = lastState.getTimeSeconds() - serviceDateAsEpoch/1000;
    int requestedTransitArrivalTime = arrivalTime - (int)differenceFromStartOfTime;
    int latestAvailableTransitArrivalTime = trip.latestArrivalTime(
        requestedTransitArrivalTime,
        fromStopIndex,
        toStopIndex
    );
    if (latestAvailableTransitArrivalTime == -1) { return -1; }
    return latestAvailableTransitArrivalTime + (int)differenceFromStartOfTime;
  }
  
}
