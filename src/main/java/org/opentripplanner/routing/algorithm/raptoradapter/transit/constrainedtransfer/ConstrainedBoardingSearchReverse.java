package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.SearchDirection;
import org.opentripplanner.raptor.util.IntIterators;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;

final class ConstrainedBoardingSearchReverse implements ConstrainedBoardingSearchStrategy {

  @Override
  public int time(RaptorTripSchedule schedule, int stopPos) {
    return schedule.arrival(stopPos);
  }

  @Override
  public boolean timeIsBefore(int time0, int time1) {
    return time0 > time1;
  }

  @Override
  public IntIterator scheduleIndexIterator(RaptorTimeTable<TripSchedule> timetable) {
    return IntIterators.intDecIterator(timetable.numberOfTripSchedules(), 0);
  }

  @Override
  public int plus(int v, int u) {
    return v - u;
  }

  @Override
  public SearchDirection direction() {
    return SearchDirection.REVERSE;
  }
}
