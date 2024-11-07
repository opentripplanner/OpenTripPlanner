package org.opentripplanner.raptor.rangeraptor.transit;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * A calculator that will take you back in time not forward, this is the basic logic to implement a
 * reveres search.
 */
public class ReverseTransitCalculator<T extends RaptorTripSchedule>
  extends ReverseTimeCalculator
  implements TransitCalculator<T> {

  @Override
  public int stopArrivalTime(T onTrip, int stopPositionInPattern, int alightSlack) {
    return plusDuration(onTrip.departure(stopPositionInPattern), alightSlack);
  }

  @Override
  public int departureTime(RaptorAccessEgress accessEgress, int departureTime) {
    return accessEgress.latestArrivalTime(departureTime);
  }

  @Override
  public boolean alightingPossibleAt(RaptorTripPattern pattern, int stopPos) {
    return pattern.boardingPossibleAt(stopPos);
  }

  @Override
  public boolean boardingPossibleAt(RaptorTripPattern pattern, int stopPos) {
    return pattern.alightingPossibleAt(stopPos);
  }
}
