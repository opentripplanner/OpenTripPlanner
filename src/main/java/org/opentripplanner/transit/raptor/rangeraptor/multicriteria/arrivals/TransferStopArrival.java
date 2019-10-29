package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;

import org.opentripplanner.transit.raptor.api.transit.TransferLeg;
import org.opentripplanner.transit.raptor.api.transit.TripScheduleInfo;

/**
 *
 * @param <T> The TripSchedule type defined by the user of the range raptor API.
 */
public final class TransferStopArrival<T extends TripScheduleInfo> extends AbstractStopArrival<T> {

    public TransferStopArrival(AbstractStopArrival<T> previousState, TransferLeg transferLeg, int arrivalTime, int additionalCost) {
        super(
                previousState,
                transferLeg.stop(),
                arrivalTime - transferLeg.durationInSeconds(),
                arrivalTime,
                previousState.travelDuration() + transferLeg.durationInSeconds(),
                additionalCost
        );
    }

    @Override
    public int transferFromStop() {
        return previousStop();
    }

    @Override
    public boolean arrivedByTransfer() {
        return true;
    }
}
