package org.opentripplanner.transit.raptor._data.stoparrival;

import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.view.AccessPathView;

public class Access extends AbstractStopArrival {

    private final RaptorTransfer access;

    public Access(int stop, int departureTime, int arrivalTime, int cost) {
        this(stop, arrivalTime, walk(stop, Math.abs(arrivalTime - departureTime), cost));
    }

    public Access(int stop, int arrivalTime, RaptorTransfer path) {
        super(0, stop, arrivalTime, path.generalizedCost(), null);
        this.access = path;
    }

    @Override public boolean arrivedByAccess() { return true; }

    @Override
    public AccessPathView accessPath() {
        return () -> access;
    }
}
