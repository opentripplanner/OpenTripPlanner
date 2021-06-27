package org.opentripplanner.transit.raptor._data.stoparrival;

import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.walkCost;

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
        int cost,
        ArrivalView<TestTripSchedule> previous
    ) {
        super(round, stop, arrivalTime, cost, previous);
        // In a reverse search we the arrival is before the departure
        this.transfer = new RaptorTransfer() {
            @Override public int stop() {
                return stop;
            }

            @Override public int durationInSeconds() {
                return Math.abs(arrivalTime - departureTime);
            }

            @Override
            public int generalizedCost() {
                return walkCost(durationInSeconds());
            }
        };
    }

    @Override public boolean arrivedByTransfer() {
        return true;
    }

    @Override public TransferPathView transferPath() {
        return () -> transfer;
    }
}
