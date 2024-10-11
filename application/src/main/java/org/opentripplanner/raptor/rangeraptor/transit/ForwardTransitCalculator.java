package org.opentripplanner.raptor.rangeraptor.transit;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * Used to calculate times in a forward trip search.
 */
public class ForwardTransitCalculator<T extends RaptorTripSchedule>
  extends ForwardTimeCalculator
  implements TransitCalculator<T> {

  public ForwardTransitCalculator() {}

  @Override
  public int stopArrivalTime(T onTrip, int stopPositionInPattern, int alightSlack) {
    return onTrip.arrival(stopPositionInPattern) + alightSlack;
  }

  @Override
  public int departureTime(RaptorAccessEgress accessEgress, int departureTime) {
    return accessEgress.earliestDepartureTime(departureTime);
  }

  @Override
  public boolean alightingPossibleAt(RaptorTripPattern pattern, int stopPos) {
    return pattern.alightingPossibleAt(stopPos);
  }

  @Override
  public boolean boardingPossibleAt(RaptorTripPattern pattern, int stopPos) {
    return pattern.boardingPossibleAt(stopPos);
  }
}
