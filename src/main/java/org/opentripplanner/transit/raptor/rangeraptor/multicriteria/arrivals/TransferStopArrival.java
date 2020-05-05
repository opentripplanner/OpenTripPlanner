package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransferStopArrival<T extends RaptorTripSchedule> extends AbstractStopArrival<T> {

    public TransferStopArrival(AbstractStopArrival<T> previousState, RaptorTransfer transferLeg, int arrivalTime, int additionalCost) {
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
    public RaptorTransfer accessEgress() {
        throw new UnsupportedOperationException("No accessEgress for transfer stop arrival");
    }

    @Override
    public boolean arrivedByTransfer() {
        return true;
    }
}
