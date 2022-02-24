package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.IntIterators;

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
}