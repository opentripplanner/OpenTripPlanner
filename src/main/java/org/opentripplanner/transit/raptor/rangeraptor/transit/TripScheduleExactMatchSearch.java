package org.opentripplanner.transit.raptor.rangeraptor.transit;

import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;


/**
 * This trip search will only match trips that is within the given slack of the timeLimit.
 * <p/>
 * Let say we want to board a trip and the 'earliest boarding time' is 12:10:00, and the slack is 60
 * seconds. Then all trip leaving from 12:10:00 to 12:11:00 is accepted. This is used to prevent
 * boarding trips that depart long after the Range Raptor search window. The Range Raptor algorithm
 * implemented here uses this wrapper for round 1, for all other rounds the normal {@link
 * TripScheduleBoardSearch} or {@link TripScheduleAlightSearch} is used.
 * <p/>
 * This class do not perform the trip search, but delegates this.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TripScheduleExactMatchSearch<T extends RaptorTripSchedule>
        implements TripScheduleSearch<T> {

    private final int slack;
    private final TripScheduleSearch<T> delegate;
    private final TransitCalculator<T> calculator;

    TripScheduleExactMatchSearch(
            TripScheduleSearch<T> delegate,
            TransitCalculator<T> calculator,
            int slack
    ) {
        this.delegate = delegate;
        this.slack = slack;
        this.calculator = calculator;
    }

    @Override
    @Nullable
    public RaptorTripScheduleBoardOrAlightEvent<T> search(
            int timeLimit,
            int stopPositionInPattern,
            int tripIndexLimit
    ) {
        RaptorTripScheduleBoardOrAlightEvent<T> result = delegate.search(
                timeLimit, stopPositionInPattern, tripIndexLimit
        );
        return result != null && isWithinSlack(timeLimit, result.getTime()) ? result : null;
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(TripScheduleExactMatchSearch.class)
                .addNum("slack", slack)
                .addObj("delegate", delegate)
                .toString();
    }

    private boolean isWithinSlack(int timeLimit, int time) {
        return calculator.isBest(time, timeLimit + slack);
    }
}
