package org.opentripplanner.transit.raptor.util;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public record ReversedRaptorTransfer(int stop, RaptorTransfer transfer) implements RaptorTransfer {

    @Override
    public int generalizedCost() {
        return transfer.generalizedCost();
    }

    @Override
    public int durationInSeconds() {
        return transfer.durationInSeconds();
    }

    @Override
    public int earliestDepartureTime(int requestedDepartureTime) {
        return transfer.earliestDepartureTime(requestedDepartureTime);
    }

    @Override
    public int latestArrivalTime(int requestedArrivalTime) {
        return transfer.latestArrivalTime(requestedArrivalTime);
    }

    @Override
    public boolean hasOpeningHours() {
        return transfer.hasOpeningHours();
    }

    @Override
    public int numberOfRides() {
        return transfer.numberOfRides();
    }

    @Override
    public boolean hasRides() {
        return transfer.hasRides();
    }

    @Override
    public boolean stopReachedOnBoard() {
        return transfer.stopReachedOnBoard();
    }
}
