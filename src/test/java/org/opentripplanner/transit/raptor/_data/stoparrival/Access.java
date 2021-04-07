package org.opentripplanner.transit.raptor._data.stoparrival;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.view.AccessPathView;

import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;

public class Access extends AbstractStopArrival {

    private final RaptorTransfer access;

    public Access(int stop, int departureTime, int arrivalTime, int cost) {
        super(0, stop, arrivalTime, cost, null);
        this.access = walk(stop, Math.abs(arrivalTime - departureTime));
    }
    @Override public boolean arrivedByAccess() { return true; }

    @Override
    public AccessPathView accessPath() {
        return () -> access;
    }
}
