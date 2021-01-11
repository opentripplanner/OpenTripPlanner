package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.TransferPathView;

/**
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransferStopArrival<T extends RaptorTripSchedule> extends AbstractStopArrival<T> {

    public TransferStopArrival(AbstractStopArrival<T> previousState, RaptorTransfer transferLeg, int arrivalTime, int additionalCost) {
        super(
                previousState,
                transferLeg.stop(),
                arrivalTime,
                additionalCost
        );
    }

    @Override
    public boolean arrivedByTransfer() {
        return true;
    }

    @Override
    public TransferPathView transferLeg() {
        return this::durationInSeconds;
    }

    private int durationInSeconds() {
        // We do not keep the reference to the TransitLayer transfer(RaptorTransfer),
        // so we compute the duration.
        return arrivalTime() - previous().arrivalTime();
    }
}
