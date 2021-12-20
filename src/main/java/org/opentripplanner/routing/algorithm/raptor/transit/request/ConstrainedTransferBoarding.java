package org.opentripplanner.routing.algorithm.raptor.transit.request;

import javax.validation.constraints.NotNull;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;


public class ConstrainedTransferBoarding<T extends RaptorTripSchedule>
        implements RaptorTripScheduleBoardOrAlightEvent<T> {

    private final TransferConstraint constraint;
    private final int tripIndex;
    private final T trip;
    private final int stopPositionInPattern;
    private final int time;

    ConstrainedTransferBoarding(
            @NotNull TransferConstraint constraint,
            int tripIndex,
            @NotNull T trip,
            int stopPositionInPattern,
            int time
    ) {
        this.constraint = constraint;
        this.tripIndex = tripIndex;
        this.trip = trip;
        this.stopPositionInPattern = stopPositionInPattern;
        this.time = time;
    }

    @Override
    public int getTripIndex() { return tripIndex; }

    @Override
    @NotNull
    public T getTrip() { return trip; }

    @Override
    public int getStopPositionInPattern() { return stopPositionInPattern; }

    @Override
    public int getTime() { return time; }

    @Override
    @NotNull
    public TransferConstraint getTransferConstraint() {
        return constraint;
    }
}
