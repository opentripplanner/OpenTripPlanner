package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.TransitArrival;
import org.opentripplanner.transit.raptor.api.view.TransferPathView;

/**
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransferStopArrival<T extends RaptorTripSchedule> extends AbstractStopArrival<T> {

    private final RaptorTransfer transfer;

    public TransferStopArrival(
        AbstractStopArrival<T> previousState,
        RaptorTransfer transferPath,
        int arrivalTime,
        int additionalCost
    ) {
        super(
                previousState,
                transferPath.stop(),
                arrivalTime,
                additionalCost
        );
        this.transfer = transferPath;
    }

    @Override
    public boolean arrivedByTransfer() {
        return true;
    }

    @Override
    public TransferPathView transferPath() {
        return () -> transfer;
    }

    @Override
    public TransitArrival<T> mostResentTransitArrival() {
        return previous().mostResentTransitArrival();
    }
}
