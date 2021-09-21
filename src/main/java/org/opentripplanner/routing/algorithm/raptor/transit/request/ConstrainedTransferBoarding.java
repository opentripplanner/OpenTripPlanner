package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;

class ConstrainedTransferBoarding implements RaptorTripScheduleBoardOrAlightEvent<TripSchedule> {

    private final int tripIndex;
    private final TripSchedule trip;
    private final int stopPositionInPattern;
    private final int time;

    ConstrainedTransferBoarding(
            int tripIndex,
            TripSchedule trip,
            int stopPositionInPattern,
            int time
    ) {
        this.tripIndex = tripIndex;
        this.trip = trip;
        this.stopPositionInPattern = stopPositionInPattern;
        this.time = time;
    }

    @Override
    public int getTripIndex() { return tripIndex; }

    @Override
    public TripSchedule getTrip() { return trip; }

    @Override
    public int getStopPositionInPattern() { return stopPositionInPattern; }

    @Override
    public int getTime() { return time; }
}
