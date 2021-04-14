package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.TripScheduleBoardOrAlightEvent;

/**
 * Basic TripScheduleBoardOrAlightEvent implementation.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class TripScheduleBoardOrAlightEventObject<T extends RaptorTripSchedule>
        implements TripScheduleBoardOrAlightEvent<T> {

    private final int earliestTime;
    private final int stopPositionInPattern;
    private final int tripIndex;
    private final T trip;

    public TripScheduleBoardOrAlightEventObject(
            int earliestTime,
            int stopPositionInPattern,
            int tripIndex,
            T trip
    ) {
        this.earliestTime = earliestTime;
        this.stopPositionInPattern = stopPositionInPattern;
        this.tripIndex = tripIndex;
        this.trip = trip;
    }

    @Override
    public final int getEarliestTime() {
        return earliestTime;
    }

    @Override
    public final int getStopPositionInPattern() {
        return stopPositionInPattern;
    }

    @Override
    public final T getTrip() {
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
        return ToStringBuilder.of(TripScheduleBoardOrAlightEventObject.class)
                .addServiceTime("earliestTime", earliestTime, -9999)
                .addNum("stopPositionInPattern", stopPositionInPattern)
                .addNum("tripIndex", tripIndex)
                .addObj("trip", trip)
                .toString();
    }
}
