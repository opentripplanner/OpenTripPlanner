package org.opentripplanner.transit.raptor._shared;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.view.AccessPathView;

public class Access extends AbstractStopArrival {

    private final RaptorTransfer access;

    public Access(int stop, int departureTime, int arrivalTime) {
        super(0, stop, arrivalTime, 1000, null);
        this.access = new TestRaptorTransfer(stop, Math.abs(arrivalTime - departureTime));
    }
    @Override public boolean arrivedByAccess() { return true; }

    @Override
    public AccessPathView accessPath() {
        return () -> access;
    }
}
