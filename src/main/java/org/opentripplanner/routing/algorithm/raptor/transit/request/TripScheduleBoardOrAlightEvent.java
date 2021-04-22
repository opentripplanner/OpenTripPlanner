package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;

/**
 * Basic TripScheduleBoardOrAlightEvent implementation.
 */
public class TripScheduleBoardOrAlightEvent implements RaptorTripScheduleBoardOrAlightEvent<TripSchedule> {

    private final int stopPositionInPattern;
    private final int tripIndex;
    private final TripSchedule trip;

    public TripScheduleBoardOrAlightEvent(
            int stopPositionInPattern,
            int tripIndex,
            TripSchedule trip
    ) {
        this.stopPositionInPattern = stopPositionInPattern;
        this.tripIndex = tripIndex;
        this.trip = trip;
    }

    @Override
    public final int getStopPositionInPattern() {
        return stopPositionInPattern;
    }

    @Override
    public final TripSchedule getTrip() {
        return trip;
    }

    @Override
    public final int getTripIndex() {
        return tripIndex;
    }

    @Override
    public final int getTime() {
        return trip.departure(stopPositionInPattern);
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(TripScheduleBoardOrAlightEvent.class)
                .addNum("stopPositionInPattern", stopPositionInPattern)
                .addNum("tripIndex", tripIndex)
                .addObj("trip", trip)
                .toString();
    }
}
