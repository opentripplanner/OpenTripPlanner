package org.opentripplanner.transit.model.trip;

import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * TODO RTM - THIS IS A PLACEHOLDER for {@link org.opentripplanner.model.TripTimeOnDate}
 */
public class TripOnDay implements RaptorTripSchedule {

  private final int tripIndex;
  private final Timetable timetable;

  public TripOnDay(int tripIndex, Timetable timetable) {
    this.tripIndex = tripIndex;
    this.timetable = timetable;
  }

  @Override
  public int tripSortIndex() {
    return 0;
  }

  @Override
  public int arrival(int stopPosInPattern) {
    return timetable.alightTime(tripIndex, stopPosInPattern);
  }

  @Override
  public int departure(int stopPosInPattern) {
    return timetable.boardTime(tripIndex, stopPosInPattern);
  }

  @Override
  public RaptorTripPattern pattern() {
    return null;
  }

  /**
   * This is used by the {@link org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculatorV2}
   * to retrieve a reluctance for the given trip. We might want to move this to pattern instead, and
   * use an array indexed by patternIndex for all values. This will allow us to compute this pr request,
   * today only the reluctance's can be changed per request, not the mapping to each trip.
   */
  public int transitReluctanceFactorIndex() {
    // TODO RTM - Decide on how to do this, and reimplement
    return 0;
  }
}
