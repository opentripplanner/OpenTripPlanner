package org.opentripplanner.transit.raptor._data.stoparrival;

import org.opentripplanner.transit.raptor._data.transit.TestTransfer;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.api.view.TransferPathView;

public class Walk extends AbstractStopArrival {
    private final RaptorTransfer transfer;

    public Walk(
        int round,
        int stop,
        int departureTime,
        int arrivalTime,
        int extraCost,
        ArrivalView<TestTripSchedule> previous
    ) {
        super(round, stop, arrivalTime, extraCost, previous);
        // In a reverse search we the arrival is before the departure
        int durationInSeconds = Math.abs(arrivalTime - departureTime);
        this.transfer = TestTransfer.walk(stop, durationInSeconds, extraCost);
    }

    public Walk(
            int round,
            int arrivalTime,
            RaptorTransfer transfer,
            ArrivalView<TestTripSchedule> previous
    ) {
        super(round, transfer.stop(), arrivalTime, transfer.generalizedCost(), previous);
        this.transfer = transfer;
    }

    @Override public boolean arrivedByTransfer() {
        return true;
    }

    @Override public TransferPathView transferPath() {
        return () -> transfer;
    }
}
