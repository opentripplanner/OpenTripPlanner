package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.IntIterators;

final class ConstrainedBoardingSearchForward
        implements ConstrainedBoardingSearchStrategy {

    @Override
    public int time(RaptorTripSchedule schedule, int stopPos) {
        return schedule.departure(stopPos);
    }

    @Override
    public boolean timeIsBefore(int time0, int time1) {
        return time0 < time1;
    }

    @Override
    public IntIterator scheduleIndexIterator(RaptorTimeTable<TripSchedule> timetable) {
        return IntIterators.intIncIterator(0, timetable.numberOfTripSchedules());
    }

    @Override
    public int plus(int v, int u) {
        return v + u;
    }
}