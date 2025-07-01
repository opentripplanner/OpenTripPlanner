package org.opentripplanner.updater.trip.gtfs;

import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;

class BackwardsDelayAlwaysPropagator implements BackwardsDelayPropagator{
  @Override
  public boolean adjustTimes(RealTimeTripTimesBuilder builder, int firstUpdatedIndex) {
    boolean hasAdjustedTimes = false;
    int delay = builder.getDepartureDelay(firstUpdatedIndex);
    if (builder.getArrivalDelay(firstUpdatedIndex) == 0) {
      builder.withArrivalDelay(firstUpdatedIndex, delay);
      hasAdjustedTimes = true;
    }
    delay = builder.getArrivalDelay(firstUpdatedIndex);
    if (delay == 0) {
      return false;
    }
    for (int i = firstUpdatedIndex - 1; i >= 0; i--) {
      hasAdjustedTimes = true;
      builder.withDepartureDelay(i, delay);
      builder.withArrivalDelay(i, delay);
    }
    return hasAdjustedTimes;
  }
}
